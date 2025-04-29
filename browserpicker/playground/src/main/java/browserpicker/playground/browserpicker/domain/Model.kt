package browserpicker.playground.browserpicker.domain

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import kotlinx.datetime.*
import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
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

@Serializable
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
    }
}

@Serializable
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


@Serializable
data class UriRecord(
    val id: Long = 0,
    val uriString: String,
    val host: String,
    val associatedHostRuleId: Long? = null,
    val timestamp: Instant,
    val uriSource: UriSource,
    val interactionAction: InteractionAction,
    val chosenBrowserPackage: String? = null,
) {
    init {
        require(uriString.isNotBlank()) { "uriString must not be blank" }
        require(isValidUri(uriString)) { "uriString must be a valid URI" }
        require(host.isNotBlank()) { "host must not be blank" }
    }

    companion object {
        fun isValidUri(uri: String): Boolean {
            return try {
                val parsedUri = uri.toUri()
                parsedUri.isAbsolute && (parsedUri.scheme == "http" || parsedUri.scheme == "https")
            } catch (e: Exception) {
                false
            }
        }
    }
}

@Serializable
data class HostRule(
    val id: Long = 0,
    val host: String,
    val uriStatus: UriStatus,
    val folderId: Long? = null,
    val preferredBrowserPackage: String? = null,
    val isPreferenceEnabled: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(host.isNotBlank()) { "host must not be blank" }
        require(uriStatus != UriStatus.UNKNOWN) { "uriStatus must not be UNKNOWN" }
        if (uriStatus == UriStatus.NONE) require(folderId == null && preferredBrowserPackage == null && !isPreferenceEnabled) { "NONE status may not have folder, preference, or enabled preference" }
        if (uriStatus == UriStatus.BLOCKED) require(preferredBrowserPackage == null && !isPreferenceEnabled) { "BLOCKED status must not have preference" }
    }
}

@Serializable
data class Folder(
    val id: Long = 0,
    val parentFolderId: Long? = null,
    val name: String,
    val type: FolderType,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "folder name must not be blank" }
        require(createdAt <= updatedAt) { "createdAt must not be after updatedAt" }
    }
    companion object {
        const val DEFAULT_BOOKMARK_ROOT_FOLDER_ID = 1L
        const val DEFAULT_BLOCKED_ROOT_FOLDER_ID = 2L
        const val DEFAULT_BOOKMARK_ROOT_FOLDER_NAME = "Bookmarks"
        const val DEFAULT_BLOCKED_ROOT_FOLDER_NAME = "Blocked"
    }
}

@Serializable
data class BrowserUsageStat(
    val browserPackageName: String,
    val usageCount: Long,
    val lastUsedTimestamp: Instant,
) {
    init {
        require(browserPackageName.isNotBlank()) { "browserPackageName must not be blank" }
        require(usageCount >= 0) { "usageCount must be non-negative" }
    }
}

enum class BrowserStatSortField {
    USAGE_COUNT, LAST_USED_TIMESTAMP
}

data class FilterOptions(
    val distinctHistoryHosts: List<String> = emptyList(),
    val distinctRuleHosts: List<String> = emptyList(),
    val distinctChosenBrowsers: List<String?> = emptyList(),
)

sealed interface HandleUriResult {
    data object Blocked: HandleUriResult
    data class OpenDirectly(val browserPackageName: String, val hostRuleId: Long?): HandleUriResult
    data class ShowPicker(val uriString: String, val host: String, val hostRuleId: Long?): HandleUriResult
    data class InvalidUri(val reason: String): HandleUriResult
}

@Immutable
data class UriHistoryQuery(
    val searchQuery: String? = null,
    val filterByUriSource: Set<UriSource>? = null,
    val filterByInteractionAction: Set<InteractionAction>? = null,
    val filterByChosenBrowser: Set<String?>? = null,
    val filterByHost: Set<String>? = null,
    val filterByDateRange: Pair<Instant, Instant>? = null,
    val sortBy: UriRecordSortField = UriRecordSortField.TIMESTAMP,
    val sortOrder: SortOrder = SortOrder.DESC,
    val groupBy: UriRecordGroupField = UriRecordGroupField.NONE,
    val groupSortOrder: SortOrder = SortOrder.ASC,
    val advancedFilters: List<UriRecordAdvancedFilterDomain> = emptyList()
) {
    companion object {
        val DEFAULT = UriHistoryQuery()
    }
}

@Immutable
sealed interface UriRecordAdvancedFilterDomain {
    /** Filter records based on whether they have an associated HostRule. */
    @Immutable
    data class HasAssociatedRule(val hasRule: Boolean) : UriRecordAdvancedFilterDomain

    /** Filter records based on their UriStatus (derived from associated HostRule). */
    // Note: This requires a JOIN in the data layer query builder.
    @Immutable
    data class HasUriStatus(val status: UriStatus) : UriRecordAdvancedFilterDomain

    // Add other domain-specific advanced filter types here as needed.
    // Examples:
    // data class IsInFolder(val folderId: Long) : UriRecordAdvancedFilterDomain
    // data class HasPreferenceEnabled(val isEnabled: Boolean) : UriRecordAdvancedFilterDomain
}

@Immutable
enum class SortOrder { ASC, DESC }

@Immutable
enum class UriRecordSortField {
    TIMESTAMP,
    URI_STRING,
    HOST,
    CHOSEN_BROWSER,
    INTERACTION_ACTION,
    URI_SOURCE
}

@Immutable
enum class UriRecordGroupField {
    NONE,
    INTERACTION_ACTION,
    CHOSEN_BROWSER,
    URI_SOURCE,
    HOST,
    DATE
}

@Immutable
sealed interface GroupKey {
    @Immutable @JvmInline value class Date(val value: LocalDate) : GroupKey
    @Immutable @JvmInline value class InteractionActionKey(val value: InteractionAction) : GroupKey
    @Immutable @JvmInline value class UriSourceKey(val value: UriSource) : GroupKey
    @Immutable @JvmInline value class HostKey(val value: String) : GroupKey
    @Immutable @JvmInline value class ChosenBrowserKey(val value: String) : GroupKey
}

fun groupKeyToStableString(key: GroupKey): String = when (key) {
    is GroupKey.Date -> "DATE_${key.value}"
    is GroupKey.InteractionActionKey -> "ACTION_${key.value.name}"
    is GroupKey.UriSourceKey -> "SOURCE_${key.value.name}"
    is GroupKey.HostKey -> "HOST_${key.value}"
    is GroupKey.ChosenBrowserKey -> "BROWSER_${key.value}"
}

data class DomainGroupCount(val groupValue: String?, val count: Int)
data class DomainDateCount(val date: Instant?, val count: Int)

