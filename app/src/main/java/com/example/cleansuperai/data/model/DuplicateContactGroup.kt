package com.example.cleansuperai.data.model

data class DuplicateContactGroup(
    val groupKey: String,
    val primaryDisplayName: String,
    val normalizedPhone: String,
    val entries: List<ContactEntry>,
)
