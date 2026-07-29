package com.aetherquorion.cleansuperai.ui.contacts

import com.aetherquorion.cleansuperai.data.model.ContactCleanupSummary

data class ContactsUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val summary: ContactCleanupSummary? = null,
    val errorMessage: String? = null,
)
