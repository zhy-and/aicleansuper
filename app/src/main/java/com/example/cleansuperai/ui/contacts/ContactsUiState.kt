package com.example.cleansuperai.ui.contacts

import com.example.cleansuperai.data.model.ContactCleanupSummary

data class ContactsUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val summary: ContactCleanupSummary? = null,
    val errorMessage: String? = null,
)
