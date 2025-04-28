package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import kotlinx.datetime.Instant

@Immutable
data class UriRecordQueryConfig(
    val searchQuery: String? = null,
    val filterByUriSource: Set<UriSource>? = null,
    val filterByInteractionAction: Set<InteractionAction>? = null,
    val filterByChosenBrowser: Set<String?>? = null,
    val filterByHost: Set<String>? = null,
    val filterByDateRange: Pair<Instant, Instant>? = null,
    val sortBy: UriRecordSortField = UriRecordSortField.TIMESTAMP,
    val sortOrder: SortOrder = SortOrder.DESC,
    val groupBy: UriRecordGroupField = UriRecordGroupField.NONE,
    val advancedFilters: List<UriRecordAdvancedFilter> = emptyList()
) {
    companion object {
        val DEFAULT = UriRecordQueryConfig()
    }
}

// --- Sorting Options ---

@Immutable
enum class SortOrder {
    ASC,
    DESC
}

@Immutable
enum class UriRecordSortField(val dbColumnName: String) {
    TIMESTAMP("timestamp"),
    URI_STRING("uri_string"),
    HOST("host"),
    CHOSEN_BROWSER("chosen_browser_package"),
    INTERACTION_ACTION("interaction_action"),
    URI_SOURCE("uri_source");
    companion object {
        private val map = entries.associateBy(UriRecordSortField::dbColumnName)
        fun fromDbColumnName(columnName: String): UriRecordSortField? = map[columnName]
    }
}


// --- Grouping Options ---

/**
 * Defines fields by which UriRecord data can be grouped.
 * Provides the necessary SQL column name or expression for grouping.
 */
@Immutable
enum class UriRecordGroupField(
    val dbColumnNameOrExpression: String?
) {
    NONE(null),
    INTERACTION_ACTION("interaction_action"),
    CHOSEN_BROWSER("chosen_browser_package"),
    URI_SOURCE("uri_source"),
    HOST("host"),
    /** Groups by date (YYYY-MM-DD) in the device's local time. */
    DATE("STRFTIME('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime')");

    companion object {
        private val map = entries.associateBy(UriRecordGroupField::dbColumnNameOrExpression)
        fun fromDbExpression(expression: String?): UriRecordGroupField? = map[expression]
    }
}

// --- Advanced Filtering ---

@Immutable
data class UriRecordAdvancedFilter(
    val customSqlCondition: String,
    val args: List<Any>
) {
    init {
        val placeholderCount = customSqlCondition.count { it == '?' }
        require(placeholderCount == args.size) {
            "Number of placeholders '?' ($placeholderCount) in customSqlCondition must match the number of args (${args.size}). " +
                    "SQL: '$customSqlCondition', Args: $args"
        }
    }
}

// --- Grouping Result Key Representation ---

/**
 * A type-safe representation of the key used for grouping query results.
 * This is typically constructed *after* retrieving grouped data from the database.
 */
@Immutable
sealed interface GroupKey {
    @Immutable @JvmInline value class Date(val value: kotlinx.datetime.LocalDate) : GroupKey
    @Immutable @JvmInline value class InteractionActionKey(val value: InteractionAction) : GroupKey
    @Immutable @JvmInline value class UriSourceKey(val value: UriSource) : GroupKey
    @Immutable @JvmInline value class HostKey(val value: String) : GroupKey
    @Immutable @JvmInline value class ChosenBrowserKey(val value: String) : GroupKey

    companion object {
        const val NULL_BROWSER_GROUP_VALUE = "browser_picker_null_browser"
        const val NULL_BROWSER_DISPLAY_NAME = "Unknown Browser"
    }
}

fun groupKeyToStableString(key: GroupKey): String = when (key) {
    is GroupKey.Date -> "DATE_${key.value}"
    is GroupKey.InteractionActionKey -> "ACTION_${key.value.name}"
    is GroupKey.UriSourceKey -> "SOURCE_${key.value.name}"
    is GroupKey.HostKey -> "HOST_${key.value}"
    is GroupKey.ChosenBrowserKey -> "BROWSER_${key.value}"
}
