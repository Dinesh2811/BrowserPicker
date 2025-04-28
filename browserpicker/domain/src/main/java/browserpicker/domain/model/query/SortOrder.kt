package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable

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
    // Add ID("uri_record_id") if direct sorting by ID is needed

    // Companion object to safely find by column name if needed elsewhere
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
    /** The SQL column name or expression used for GROUP BY and SELECT. Null if grouping is disabled. */
    val dbColumnNameOrExpression: String?
) {
    NONE(null),
    INTERACTION_ACTION("interaction_action"),
    CHOSEN_BROWSER("chosen_browser_package"),
    URI_SOURCE("uri_source"),
    HOST("host"),
    /** Groups by date (YYYY-MM-DD) in the device's local time. */
    DATE("STRFTIME('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime')");

    // Companion object to safely find by expression if needed elsewhere
    companion object {
        private val map = entries.associateBy(UriRecordGroupField::dbColumnNameOrExpression)
        fun fromDbExpression(expression: String?): UriRecordGroupField? = map[expression]
    }
}

// --- Advanced Filtering ---

/**
 * Represents an advanced, custom filter condition to be appended to the WHERE clause.
 * Use with caution, as this allows arbitrary SQL snippets.
 *
 * @property customSqlCondition A valid SQL boolean expression (e.g., "LENGTH(uri_string) > ?").
 *                              Use '?' for bind arguments.
 * @property args A list of arguments corresponding to the '?' placeholders in [customSqlCondition].
 *                The size of this list MUST match the number of '?'.
 *                The types of arguments MUST match the expected types for the SQL comparison.
 */
@Immutable
data class UriRecordAdvancedFilter(
    val customSqlCondition: String,
    val args: List<Any> // Keep Any, as Room/SQLite handles basic type affinity
) {
    init {
        val placeholderCount = customSqlCondition.count { it == '?' }
        require(placeholderCount == args.size) {
            "Number of placeholders '?' ($placeholderCount) in customSqlCondition must match the number of args (${args.size}). " +
                    "SQL: '$customSqlCondition', Args: $args"
        }
        // Basic type validation could be added here if desired, but complex type checking is hard
        // e.g., check if args contains only String, Long, Int, Double, ByteArray, null
    }
}