package com.example.cleansuperai.data.model

data class ContactEntry(
    val contactId: Long,
    val rawContactId: Long,
    val lookupKey: String,
    val displayName: String,
    val rawPhone: String,
    val normalizedPhone: String,
)
