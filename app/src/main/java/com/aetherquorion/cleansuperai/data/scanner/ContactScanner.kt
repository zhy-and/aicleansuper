package com.aetherquorion.cleansuperai.data.scanner

import com.aetherquorion.cleansuperai.data.model.ContactCleanupSummary
import com.aetherquorion.cleansuperai.data.model.DuplicateContactGroup

interface ContactScanner {
    suspend fun loadSummary(): ContactCleanupSummary
    suspend fun mergeGroup(group: DuplicateContactGroup)
    suspend fun deleteEmptyContacts(contactIds: List<Long>)
}
