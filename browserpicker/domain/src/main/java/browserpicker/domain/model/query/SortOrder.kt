package browserpicker.domain.model.query

enum class SortOrder {
    ASC,
    DESC
}

enum class LogSortField(val dbColumnName: String) {
    TIMESTAMP("intercepted_timestamp"),
    URI("intercepted_uri"),
    BROWSER_PACKAGE("chosen_browser_package_name"),
    ACTION("action_taken"),
    URI_SOURCE("uri_source")
}

enum class LogGroupField(val dbColumnName: String?) {
    NONE(null),
    ACTION("action_taken"),
    BROWSER("chosen_browser_package_name"),
    URI_SOURCE("uri_source"),
    DATE("DATE(intercepted_timestamp / 1000, 'unixepoch')")
}

data class LogAdvancedFilter(
    val customSqlCondition: String, // e.g. "action_taken IN (?, ?)" OR "LENGTH(intercepted_uri) > ?"
    val args: List<Any>
) {
    // Basic validation to ensure placeholders match arg count (optional but helpful)
    init {
        require(customSqlCondition.count { it == '?' } == args.size) {
            "Number of placeholders '?' in customSqlCondition must match the number of args. " +
                    "SQL: '$customSqlCondition', Args: $args"
        }
    }
}