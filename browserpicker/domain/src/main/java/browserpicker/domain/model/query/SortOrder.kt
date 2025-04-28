package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable

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
    URI_SOURCE("uri_source")
    // Add more fields if needed, e.g., ID("uri_record_id")
}

@Immutable
enum class UriRecordGroupField(val dbColumnNameOrExpression: String?) {
    NONE(null),
    INTERACTION_ACTION("interaction_action"),
    CHOSEN_BROWSER("chosen_browser_package"),
    URI_SOURCE("uri_source"),
    HOST("host"),
    DATE("STRFTIME('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime')")
}

@Immutable
data class UriRecordAdvancedFilter(
    val customSqlCondition: String,
    val args: List<Any>
) {
    init {
        require(customSqlCondition.count { it == '?' } == args.size) {
            "Number of placeholders '?' in customSqlCondition must match the number of args. " +
                    "SQL: '$customSqlCondition', Args: $args"
        }
    }
}