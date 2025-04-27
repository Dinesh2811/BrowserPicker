package browserpicker.domain.model.query

import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource

data class LogQueryConfig(
    val searchQuery: String? = null,
    val filterByUriSource: List<UriSource>? = null,
    val filterByAction: List<InteractionAction>? = null,
    val filterByBrowser: List<String>? = null,
    val filterByDateRange: Pair<Long, Long>? = null,
    val sortBy: LogSortField = LogSortField.TIMESTAMP,
    val sortOrder: SortOrder = SortOrder.DESC,
    val groupBy: LogGroupField = LogGroupField.NONE,
    val advancedFilters: List<LogAdvancedFilter> = emptyList()
) {
    companion object {
        val DEFAULT = LogQueryConfig()
    }
}
