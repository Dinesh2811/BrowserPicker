package browserpicker.domain.usecase.history

import androidx.annotation.Keep
import browserpicker.domain.model.DomainDateCount
import browserpicker.domain.model.DomainGroupCount
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject

@Keep
data class HistoryOverview(
    val totalCount: Int,
    val groupCounts: List<DomainGroupCount>,
    val dateCounts: List<DomainDateCount>,
)

interface GetHistoryOverviewUseCase {
    operator fun invoke(query: UriHistoryQuery = UriHistoryQuery.DEFAULT): Flow<HistoryOverview>
}

@OptIn(ExperimentalCoroutinesApi::class)
class GetHistoryOverviewUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
): GetHistoryOverviewUseCase {
    override fun invoke(query: UriHistoryQuery): Flow<HistoryOverview> {
        Timber.d("Getting history overview with query: $query")
        // Combine multiple flows into one overview object
        return combine(
            repository.getTotalUriRecordCount(query),
            repository.getGroupCounts(query),
            repository.getDateCounts(query)
        ) { total, groups, dates ->
            HistoryOverview(
                totalCount = total,
                groupCounts = groups,
                dateCounts = dates
            )
        }.distinctUntilChanged() // Avoid unnecessary updates if underlying data hasn't changed results
            .catch { e ->
                Timber.e(e, "Error getting history overview")
                emit(HistoryOverview(0, emptyList(), emptyList())) // Emit default on error
            }
    }
}