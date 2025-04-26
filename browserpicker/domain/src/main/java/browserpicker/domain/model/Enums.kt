package browserpicker.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep @Serializable
enum class UriSource(val value: Int) {
    UNKNOWN(-1),
    INTENT(1),
    CLIPBOARD(2),
    MANUAL(3);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value }?: UNKNOWN
    }
}

@Keep @Serializable
enum class InteractionAction(val value: Int) {
    UNKNOWN(-1),
    DISMISSED(1),                       // Picker dismissed without action
    BLOCKED_URI_ENFORCED(2),            // Blocked automatically by a rule

    PREFERENCE_SET(10),                 // User set a browser preference
    OPENED_ONCE(11),                    // User picked a browser for this instance
    OPENED_BY_PREFERENCE(12);           // Opened automatically using a saved browser preference

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value }?: UNKNOWN
    }
}

@Keep @Serializable
enum class RuleType(val value: Int) {
    UNKNOWN(-1),
    NONE(0),
    BOOKMARK(1),
    BLOCK(2);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value }?: UNKNOWN
    }
}

@Keep @Serializable
enum class FolderType(val value: Int) {
    BOOKMARK(1),
    BLOCK(2);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value }?: throw IllegalArgumentException("Unknown FolderType value: $value")
        fun fromValueOrNull(value: Int) = entries.find { it.value == value }
    }
}
