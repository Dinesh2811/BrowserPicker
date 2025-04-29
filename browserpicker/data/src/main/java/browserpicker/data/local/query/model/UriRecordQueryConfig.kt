package browserpicker.data.local.query.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.model.query.UriRecordAdvancedFilterDomain
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordSortField
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
    val groupSortOrder: SortOrder = SortOrder.ASC,
//    val advancedFilters: List<UriRecordAdvancedFilter> = emptyList(),
    val advancedFilters: List<UriRecordAdvancedFilterDomain> = emptyList(),
) {
    companion object {
        val DEFAULT = UriRecordQueryConfig()
    }
}

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

@Serializable
data class GroupCount(
    @ColumnInfo(name = "groupValue")
    val groupValue: String?,
    @ColumnInfo(name = "count")
    val count: Int
)

@Serializable
data class DateCount(
    @ColumnInfo(name = "date")
    val date: Instant?,
    @ColumnInfo(name = "count")
    val count: Int
)
