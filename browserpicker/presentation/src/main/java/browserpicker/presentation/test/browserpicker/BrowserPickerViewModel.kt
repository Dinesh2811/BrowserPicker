package browserpicker.presentation.test.browserpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.RecordBrowserUsageUseCase
import browserpicker.domain.usecases.uri.host.GetHostRuleUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import browserpicker.domain.usecases.uri.shared.HandleUriUseCase
import browserpicker.domain.usecases.uri.shared.RecordUriInteractionUseCase
import browserpicker.domain.usecases.uri.shared.ValidateUriUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Browser Picker screen.
 * 
 * This is the core ViewModel that handles:
 * - Processing incoming URIs
 * - Displaying available browsers
 * - Handling user browser selection
 * - Setting browser preferences for hosts
 * - Recording URI interactions
 * 
 * Used by: BrowserPickerScreen
 */
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    private val handleUriUseCase: HandleUriUseCase,
    private val validateUriUseCase: ValidateUriUseCase,
    private val getAvailableBrowsersUseCase: GetAvailableBrowsersUseCase,
    private val getMostFrequentlyUsedBrowserUseCase: GetMostFrequentlyUsedBrowserUseCase,
    private val recordBrowserUsageUseCase: RecordBrowserUsageUseCase,
    private val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    private val getHostRuleUseCase: GetHostRuleUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserPickerUiState())
    val uiState: StateFlow<BrowserPickerUiState> = _uiState.asStateFlow()
    
    /**
     * Process a new URI
     */
    fun processUri(uriString: String, source: UriSource = UriSource.INTENT) {
        // Implementation will go here
    }
    
    /**
     * Handle user selection of a browser
     */
    fun selectBrowser(browserPackageName: String, setAsPreferred: Boolean = false) {
        // Implementation will go here
    }
    
    /**
     * Dismiss the picker without selecting a browser
     */
    fun dismiss() {
        // Implementation will go here
    }
    
    /**
     * Block the current URI
     */
    fun blockUri() {
        // Implementation will go here
    }
    
    /**
     * Bookmark the current URI
     */
    fun bookmarkUri() {
        // Implementation will go here
    }
}

/**
 * UI state for the Browser Picker screen
 */
data class BrowserPickerUiState(
    val uriString: String = "",
    val host: String = "",
    val availableBrowsers: List<BrowserAppInfo> = emptyList(),
    val mostUsedBrowser: BrowserAppInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBlocked: Boolean = false,
    val isBookmarked: Boolean = false,
    val hasPreferredBrowser: Boolean = false,
    val preferredBrowserPackageName: String? = null,
    val hostRuleId: Long? = null,
    val resultState: PickerResultState? = null
)

/**
 * Result state for the Browser Picker
 */
sealed class PickerResultState {
    data class OpenInBrowser(val browserPackageName: String, val uriString: String) : PickerResultState()
    data object Blocked : PickerResultState()
    data object Dismissed : PickerResultState()
} 