package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import kotlinx.datetime.Instant

@Immutable
data class UriRecordQueryConfig(
    val searchQuery: String? = null,
    val filterByUriSource: Set<UriSource>? = null, // Use Set for uniqueness
    val filterByInteractionAction: Set<InteractionAction>? = null, // Use Set
    val filterByChosenBrowser: Set<String?>? = null, // Use Set, allows filtering for null
    val filterByHost: Set<String>? = null, // Use Set
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