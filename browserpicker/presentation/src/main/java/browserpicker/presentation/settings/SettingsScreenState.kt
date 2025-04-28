package browserpicker.presentation.settings

import androidx.compose.runtime.Immutable
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.UserMessage

@Immutable
data class SettingsScreenState(
    val isLoading: Boolean = false, // Could be used for async operations, less critical here
    val userMessages: List<UserMessage> = emptyList(),
    val dialogState: SettingsDialogState = SettingsDialogState.Hidden,
    // Add other state fields for settings toggles, inputs etc. later
)

@Immutable
sealed interface SettingsDialogState {
    data object Hidden : SettingsDialogState
    data object ShowClearHistoryConfirmation : SettingsDialogState
    data object ShowClearStatsConfirmation : SettingsDialogState
    // Add other confirmation dialog states as needed
}
