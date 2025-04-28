package browserpicker.presentation.history

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.query.FilterOptions
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.repository.DomainDateCount
import browserpicker.domain.repository.DomainGroupCount
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Immutable
data class HistoryScreenState(
    val historyPagingDataFlow: Flow<PagingData<UriRecord>> = emptyFlow(), // Cached flow for Compose Paging
    val query: UriHistoryQuery = UriHistoryQuery.DEFAULT,
    val overview: HistoryOverviewState = HistoryOverviewState(),
    val filterOptions: FilterOptions? = null,
    val filterOptionsLoading: LoadingStatus = LoadingStatus.IDLE,
    val userMessages: List<UserMessage> = emptyList()
)

@Immutable
data class HistoryOverviewState(
    val loadingStatus: LoadingStatus = LoadingStatus.IDLE,
    val totalCount: Int = 0,
    val groupCounts: List<DomainGroupCount> = emptyList(),
    val dateCounts: List<DomainDateCount> = emptyList(),
    // Add calculated insights here later if needed
    val activeGrouping: UriRecordGroupField = UriRecordGroupField.NONE
)
