package com.dinesh.browserpicker.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.query.UriHistoryQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import browserpicker.domain.repository.UriHistoryRepository
import kotlinx.coroutines.flow.*

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val mockDataGenerator: MockDataGenerator,
    private val uriHistoryRepository: UriHistoryRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isDatabaseEmpty = MutableStateFlow(true)
    val isDatabaseEmpty: StateFlow<Boolean> = _isDatabaseEmpty.asStateFlow()


    init {
        viewModelScope.launch {
            uriHistoryRepository.getTotalUriRecordCount(UriHistoryQuery.DEFAULT)
                .distinctUntilChanged()
                .collect { count ->
                    Timber.d("Database URI record count changed: $count")
                    _isDatabaseEmpty.value = count == 0
                }
        }
    }

    fun generateMockData(count: Int = 1000) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _message.value = "Generating $count mock records..."
            try {
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
