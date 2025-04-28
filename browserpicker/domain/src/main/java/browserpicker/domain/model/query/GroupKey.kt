package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource

// --- Grouping Result Key Representation ---

/**
 * A type-safe representation of the key used for grouping query results.
 * This is typically constructed *after* retrieving grouped data from the database.
 */
@Immutable
sealed interface GroupKey {
    // Note: Using kotlinx.datetime.LocalDate for Date representation
    @Immutable @JvmInline value class Date(val value: kotlinx.datetime.LocalDate) : GroupKey
    @Immutable @JvmInline value class InteractionActionKey(val value: InteractionAction) : GroupKey
    @Immutable @JvmInline value class UriSourceKey(val value: UriSource) : GroupKey
    @Immutable @JvmInline value class HostKey(val value: String) : GroupKey // Keep String for host
    @Immutable @JvmInline value class ChosenBrowserKey(val value: String) : GroupKey // Keep String for package

    companion object {
        /** Special value used internally and in query results to represent a null browser package name. */
        const val NULL_BROWSER_GROUP_VALUE = "browser_picker_null_browser" // More unique than "Unknown Browser"
        /** Display name often used for the null browser group in UI. */
        const val NULL_BROWSER_DISPLAY_NAME = "Unknown Browser"
    }
}

// --- Utility Function (Consider placement - e.g., data.mapper or ui.mapper) ---
// Placed here for visibility for now, but might move later.
/**
 * Converts a [GroupKey] into a stable String representation, suitable for use
 * as keys in maps or UI lists (e.g., LazyColumn keys).
 */
fun groupKeyToStableString(key: GroupKey): String = when (key) {
    is GroupKey.Date -> "DATE_${key.value}" // ISO format is stable
    is GroupKey.InteractionActionKey -> "ACTION_${key.value.name}"
    is GroupKey.UriSourceKey -> "SOURCE_${key.value.name}"
    is GroupKey.HostKey -> "HOST_${key.value}"
    // Use the specific value from ChosenBrowserKey, which might be the NULL_BROWSER_GROUP_VALUE
    is GroupKey.ChosenBrowserKey -> "BROWSER_${key.value}"
}
