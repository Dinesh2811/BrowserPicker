package browserpicker.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable @Serializable
enum class UriSource(val value: Int) {
    INTENT(1),
    CLIPBOARD(2),
    MANUAL(3);

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromValue(value: Int): UriSource = entries.find { it.value == value }
            ?: throw IllegalArgumentException("Invalid UriSource value: $value. Valid values are: ${entries.joinToString { "${it.name}(${it.value})" }}")
        fun fromValueOrNull(value: Int): UriSource? = entries.find { it.value == value }
        fun isValidValue(value: Int): Boolean = entries.associateBy { it.value }.containsKey(value)
    }
}

@Immutable @Serializable
enum class InteractionAction(val value: Int) {
    UNKNOWN(-1),
    DISMISSED(1),                       // Picker dismissed without action
    BLOCKED_URI_ENFORCED(2),            // Blocked automatically by a rule

    PREFERENCE_SET(10),                 // User set a browser preference
    OPENED_ONCE(11),                    // User picked a browser for this instance
    OPENED_BY_PREFERENCE(12);           // Opened automatically using a saved browser preference

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value }?: UNKNOWN
        fun fromValueOrNull(value: Int): InteractionAction? = entries.find { it.value == value }
        fun isValidValue(value: Int): Boolean = entries.associateBy { it.value }.containsKey(value)
        fun InteractionAction.isOpenAction(): Boolean = this == OPENED_ONCE || this == OPENED_BY_PREFERENCE
    }
}

@Immutable @Serializable
enum class UriStatus(val value: Int) {
    UNKNOWN(-1),
    NONE(0),
    BOOKMARKED(1),
    BLOCKED(2);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value }?: UNKNOWN
        fun fromValueOrNull(value: Int): UriStatus? = entries.find { it.value == value }
        fun isValidValue(value: Int): Boolean = entries.associateBy { it.value }.containsKey(value)
        fun UriStatus.isActive(): Boolean = this != UNKNOWN && this != NONE

        fun UriStatus.toFolderType(): FolderType? = when (this) {
            UriStatus.BOOKMARKED -> FolderType.BOOKMARK
            UriStatus.BLOCKED -> FolderType.BLOCK
            else -> null
        }
    }
}

@Immutable @Serializable
enum class FolderType(val value: Int) {
    BOOKMARK(1),
    BLOCK(2);

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromValue(value: Int) = entries.find { it.value == value }?: throw IllegalArgumentException("Unknown FolderType value: $value")
        fun fromValueOrNull(value: Int) = entries.find { it.value == value }
        fun isValidValue(value: Int): Boolean = entries.associateBy { it.value }.containsKey(value)
        fun FolderType.toUriStatus(): UriStatus = when (this) {
            BOOKMARK -> UriStatus.BOOKMARKED
            BLOCK -> UriStatus.BLOCKED
        }
    }
}
