package browserpicker.presentation.picker

// package browserpicker.presentation.picker

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.*
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.UserMessage

// Dummy Data Class (provided by user)
data class BrowserAppInfo(
    val appName: String,
    val packageName: String,
)

@Immutable
data class PickerScreenState(
    val isLoading: Boolean = true,
    val uriString: String = "",
    val host: String = "",
    val source: UriSource = UriSource.INTENT, // Need source for recording interaction
    val associatedHostRuleId: Long? = null,
    val browsers: List<BrowserAppInfo> = emptyList(),
    val currentRule: HostRule? = null,
    val availableBookmarkFolders: List<Folder> = emptyList(), // Only bookmark folders
    val userMessages: List<UserMessage> = emptyList(),
    val closeSignal: Boolean = false // Simple signal to close the picker screen
)

// Alternative: Events via SharedFlow
// sealed interface PickerEvent {
//     data object ClosePicker : PickerEvent
//     // data class ShowMessage(val message: String) : PickerEvent // Can use UiState messages instead
// }