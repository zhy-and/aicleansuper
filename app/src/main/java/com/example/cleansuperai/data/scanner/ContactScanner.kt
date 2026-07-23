package com.example.cleansuperai.data.scanner

import com.example.cleansuperai.data.model.ContactCleanupSummary
import com.example.cleansuperai.data.model.DuplicateContactGroup

interface ContactScanner {
    suspend fun loadSummary(): ContactCleanupSummary
    suspend fun mergeGroup(group: DuplicateContactGroup)
    suspend fun deleteEmptyContacts(contactIds: List<Long>)
}
