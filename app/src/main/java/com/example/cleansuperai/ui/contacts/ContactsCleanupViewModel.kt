package com.example.cleansuperai.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleansuperai.data.model.DuplicateContactGroup
import com.example.cleansuperai.data.scanner.AndroidContactScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsCleanupViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner by lazy { AndroidContactScanner(application.applicationContext) }

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    fun updatePermission(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
        if (hasPermission && _uiState.value.summary == null && !_uiState.value.isLoading) {
            loadSummary()
        }
    }

    fun loadSummary() {
        if (!_uiState.value.hasPermission || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { scanner.loadSummary() }
                .onSuccess { summary ->
                    _uiState.value = ContactsUiState(
                        hasPermission = true,
                        isLoading = false,
                        summary = summary,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = ContactsUiState(
                        hasPermission = true,
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
        }
    }

    fun mergeGroup(group: DuplicateContactGroup, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                scanner.mergeGroup(group)
                true
            }.getOrDefault(false)
            if (success) {
                loadSummary()
            }
            onComplete(success)
        }
    }

    fun deleteEmptyContacts(contactIds: List<Long>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                scanner.deleteEmptyContacts(contactIds)
                true
            }.getOrDefault(false)
            if (success) {
                loadSummary()
            }
            onComplete(success)
        }
    }
}
