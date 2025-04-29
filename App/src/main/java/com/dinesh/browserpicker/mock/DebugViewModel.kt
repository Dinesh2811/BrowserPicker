package com.dinesh.browserpicker.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Example Usage (e.g., in a DebugViewModel or initialization logic)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val mockDataGenerator: MockDataGenerator
    // ... other dependencies
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun generateMockData(count: Int = 1000) {
        if (_isLoading.value) return // Prevent concurrent runs

        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Generating $count mock records..."
            try {
                // Call the generator
                mockDataGenerator.generate(uriRecordCount = count)
                _message.value = "Mock data generation complete!"
            } catch (e: Exception) {
                Timber.e(e, "Mock data generation failed")
                _message.value = "Error generating mock data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMockData() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Clearing mock data..."
            try {
                mockDataGenerator.clearAllData()
                _message.value = "Mock data cleared."
            } catch (e: Exception) {
                Timber.e(e, "Mock data clearing failed")
                _message.value = "Error clearing mock data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
