package com.aetherquorion.cleansuperai.data.scanner

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.aetherquorion.cleansuperai.data.model.ContactCleanupSummary
import com.aetherquorion.cleansuperai.data.model.ContactEntry
import com.aetherquorion.cleansuperai.data.model.DuplicateContactGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidContactScanner(
    private val context: Context,
) : ContactScanner {

    override suspend fun loadSummary(): ContactCleanupSummary = withContext(Dispatchers.IO) {
        val phoneEntries = loadPhoneEntries()
        val duplicateGroups = phoneEntries
            .filter { it.normalizedPhone.isNotBlank() }
            .groupBy { it.normalizedPhone }
            .values
            .filter { entries -> entries.map { it.contactId }.distinct().size >= 2 }
            .map { entries ->
                val sorted = entries.sortedByDescending { it.displayName.length }
                DuplicateContactGroup(
                    groupKey = sorted.first().normalizedPhone,
                    primaryDisplayName = sorted.first().displayName.ifBlank { "未命名联系人" },
                    normalizedPhone = sorted.first().normalizedPhone,
                    entries = sorted.distinctBy { it.contactId },
                )
            }
            .sortedByDescending { it.entries.size }

        val emptyContacts = loadEmptyContacts()
        ContactCleanupSummary(
            duplicateGroups = duplicateGroups,
            emptyContacts = emptyContacts,
        )
    }

    override suspend fun mergeGroup(group: DuplicateContactGroup) = withContext(Dispatchers.IO) {
        val rawIds = group.entries.map { it.rawContactId }.distinct()
        if (rawIds.size < 2) return@withContext

        val operations = arrayListOf<ContentProviderOperation>()
        val anchor = rawIds.first()
        rawIds.drop(1).forEach { rawId ->
            operations += ContentProviderOperation
                .newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                .withValue(
                    ContactsContract.AggregationExceptions.TYPE,
                    ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER,
                )
                .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, anchor)
                .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, rawId)
                .build()
        }
        if (operations.isNotEmpty()) {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        }
    }

    override suspend fun deleteEmptyContacts(contactIds: List<Long>) = withContext(Dispatchers.IO) {
        contactIds.distinct().forEach { contactId ->
            val uri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendPath(contactId.toString())
                .build()
            context.contentResolver.delete(uri, null, null)
        }
    }

    private fun loadPhoneEntries(): List<ContactEntry> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )

        val result = mutableListOf<ContactEntry>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val rawContactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
            val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val rawPhone = cursor.getString(numberIndex).orEmpty()
                result += ContactEntry(
                    contactId = cursor.getLong(contactIdIndex),
                    rawContactId = cursor.getLong(rawContactIdIndex),
                    lookupKey = cursor.getString(lookupIndex).orEmpty(),
                    displayName = cursor.getString(nameIndex).orEmpty(),
                    rawPhone = rawPhone,
                    normalizedPhone = normalizePhone(rawPhone),
                )
            }
        }
        return result
    }

    private fun loadEmptyContacts(): List<ContactEntry> {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        )

        val result = mutableListOf<ContactEntry>()
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex).orEmpty()
                val hasPhone = cursor.getInt(hasPhoneIndex) > 0
                if (name.isBlank() || !hasPhone) {
                    result += ContactEntry(
                        contactId = cursor.getLong(idIndex),
                        rawContactId = cursor.getLong(idIndex),
                        lookupKey = cursor.getString(lookupIndex).orEmpty(),
                        displayName = name.ifBlank { "未命名联系人" },
                        rawPhone = "",
                        normalizedPhone = "",
                    )
                }
            }
        }
        return result
    }

    private fun normalizePhone(phone: String): String {
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        return cleaned.removePrefix("86").removePrefix("+86")
    }
}
