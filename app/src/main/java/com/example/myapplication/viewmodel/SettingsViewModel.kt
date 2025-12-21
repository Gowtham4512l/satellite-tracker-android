package com.example.myapplication.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class SettingsUiState(
    val apiKey: String = "",
    val bleMacAddress: String = "",
    val apiKeyError: String? = null,
    val bleMacError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isApiKeyValid: Boolean = false,
    val isBleMacValid: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var repository: SettingsRepository? = null

    fun initialize(context: Context) {
        repository = SettingsRepository(context.applicationContext)
        loadCurrentSettings()
    }

    /**
     * Load current settings from repository
     */
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            val repo = repository ?: return@launch
            
            val apiKey = repo.getApiKey() ?: ""
            val bleMac = repo.getBleMacAddress() ?: ""
            
            _uiState.update {
                it.copy(
                    apiKey = apiKey,
                    bleMacAddress = bleMac,
                    isApiKeyValid = repo.validateApiKey(apiKey),
                    isBleMacValid = bleMac.isBlank() || repo.validateMacAddress(bleMac)
                )
            }
        }
    }

    /**
     * Update API key input
     */
    fun onApiKeyChange(newApiKey: String) {
        val repo = repository ?: return
        val isValid = repo.validateApiKey(newApiKey)
        
        _uiState.update {
            it.copy(
                apiKey = newApiKey,
                apiKeyError = null,
                isApiKeyValid = isValid,
                saveSuccess = false
            )
        }
    }

    /**
     * Update BLE MAC address input
     */
    fun onBleMacChange(newMac: String) {
        val repo = repository ?: return
        val trimmed = newMac.trim().uppercase()
        val isValid = trimmed.isBlank() || repo.validateMacAddress(trimmed)
        
        _uiState.update {
            it.copy(
                bleMacAddress = trimmed,
                bleMacError = null,
                isBleMacValid = isValid,
                saveSuccess = false
            )
        }
    }

    /**
     * Save API key
     */
    fun saveApiKey() {
        viewModelScope.launch {
            val repo = repository ?: return@launch
            val apiKey = _uiState.value.apiKey.trim()
            
            _uiState.update { it.copy(isSaving = true, apiKeyError = null) }
            
            val result = repo.saveApiKey(apiKey)
            
            result.fold(
                onSuccess = {
                    Timber.d("API key saved successfully")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            apiKeyError = null
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to save API key")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            apiKeyError = error.message ?: "Invalid API key format"
                        )
                    }
                }
            )
        }
    }

    /**
     * Save BLE MAC address
     */
    fun saveBleMac() {
        viewModelScope.launch {
            val repo = repository ?: return@launch
            val mac = _uiState.value.bleMacAddress.trim()
            
            // Allow empty MAC (user might not have BLE device)
            if (mac.isBlank()) {
                Timber.d("BLE MAC address cleared")
                _uiState.update {
                    it.copy(
                        saveSuccess = true,
                        bleMacError = null
                    )
                }
                return@launch
            }
            
            _uiState.update { it.copy(isSaving = true, bleMacError = null) }
            
            val result = repo.saveBleMacAddress(mac)
            
            result.fold(
                onSuccess = {
                    Timber.d("BLE MAC address saved successfully")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            bleMacError = null
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to save BLE MAC address")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            bleMacError = error.message ?: "Invalid MAC address format"
                        )
                    }
                }
            )
        }
    }

    /**
     * Save all settings
     */
    fun saveAllSettings() {
        viewModelScope.launch {
            saveApiKey()
            saveBleMac()
        }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    /**
     * Check if settings are valid for app to function
     */
    fun areSettingsValid(): Boolean {
        return _uiState.value.isApiKeyValid
    }
}
