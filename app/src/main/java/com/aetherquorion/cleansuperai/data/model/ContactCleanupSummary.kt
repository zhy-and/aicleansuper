package com.aetherquorion.cleansuperai.data.model

data class ContactCleanupSummary(
    val duplicateGroups: List<DuplicateContactGroup>,
    val emptyContacts: List<ContactEntry>,
)
