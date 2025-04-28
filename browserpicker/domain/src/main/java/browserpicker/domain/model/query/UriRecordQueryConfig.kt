package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import kotlinx.datetime.Instant

@Immutable
data class UriRecordQueryConfig(
    val searchQuery: String? = null,
    val filterByUriSource: List<UriSource>? = null,
    val filterByInteractionAction: List<InteractionAction>? = null,
    val filterByChosenBrowser: List<String?>? = null, // Allow filtering for null browser
    val filterByHost: List<String>? = null,
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