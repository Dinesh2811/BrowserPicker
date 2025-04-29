package browserpicker.domain.model.query

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant

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
    // Use domain-specific advanced filters instead of raw SQL structure
    val advancedFilters: List<UriRecordAdvancedFilterDomain> = emptyList()
) {
    companion object {
        val DEFAULT = UriHistoryQuery()
    }
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
