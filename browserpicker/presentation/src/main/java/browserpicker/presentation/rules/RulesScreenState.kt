package browserpicker.presentation.rules

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.*
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.UserMessage

// Represents the state of the Rules screen (can be filtered for Bookmarks or Blocked)
@Immutable
data class RulesScreenState(
    val isLoading: Boolean = false,
    val rules: List<HostRule> = emptyList(),
    val folders: List<Folder> = emptyList(), // Folders of the relevant type
    val currentType: UriStatus = UriStatus.BOOKMARKED, // BOOKMARKED or BLOCKED
    val selectedFolderId: Long? = null, // Filter by specific folder (null means root/all)
    val userMessages: List<UserMessage> = emptyList(),
    val dialogState: RuleDialogState = RuleDialogState.Hidden
)

// Represents the state of the Add/Edit Rule dialog
@Immutable
sealed interface RuleDialogState {
    data object Hidden : RuleDialogState
    data class Add(
        val host: String = "", // Pre-fill if possible
        val availableFolders: List<Folder> = emptyList(),
        val currentType: UriStatus // BOOKMARKED or BLOCKED
    ) : RuleDialogState
    data class Edit(
        val rule: HostRule,
        val availableFolders: List<Folder> = emptyList()
    ) : RuleDialogState
}
