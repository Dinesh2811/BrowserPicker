package browserpicker.presentation.navigation

import browserpicker.domain.model.FolderType
import browserpicker.domain.model.UriStatus
import kotlinx.serialization.Serializable

// Using @Serializable for type-safe navigation arguments
// Define all possible navigation destinations within the main app UI

@Serializable
object History // Main history screen

@Serializable
data class Rules(
    // Use value for serialization compatibility if needed, or rely on built-in enum support
    val typeValue: Int = UriStatus.BOOKMARKED.value // Default to bookmarks
) {
    // Convenience property to get the actual enum type
    val type: UriStatus get() = UriStatus.fromValue(typeValue)
}

@Serializable
data class Folders(
    val typeValue: Int = FolderType.BOOKMARK.value // Default to bookmark folders
) {
    val type: FolderType get() = FolderType.fromValue(typeValue)
}

@Serializable
object Stats // Browser stats screen

@Serializable
object Settings // Placeholder for settings screen

// Note: The Picker UI itself might be better suited as a separate Activity/Dialog
// triggered by an Intent filter result. However, if we WERE to navigate to it
// within this NavHost (e.g., for re-picking), it might look like this:
// @Serializable
// data class Picker(
//     val uriString: String,
//     val host: String,
//     val sourceValue: Int,
//     val ruleId: Long?
// )
