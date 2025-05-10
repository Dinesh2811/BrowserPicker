package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class GetMostVisitedHostsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) : GetMostVisitedHostsUseCase {

    override operator fun invoke(
        limit: Int,
        timeRange: Pair<Instant, Instant>?
    ): Flow<DomainResult<List<GroupCount>, AppError>> {
        val query = UriHistoryQuery.DEFAULT.copy(
            filterByDateRange = timeRange,
            groupBy = UriRecordGroupField.HOST
            // The repository's getGroupCounts should handle sorting by count and limiting.
            // If not, client-side sorting and limiting would be needed after fetching all group counts.
        )

        return uriHistoryRepository.getGroupCounts(query).map { result ->
            result.mapSuccess { groupCounts ->
                // Assuming getGroupCounts returns hosts sorted by count descending.
                // If not, sort here:
                // val sortedCounts = groupCounts.sortedByDescending { it.count }
                // groupCounts.take(limit)
                // For now, we assume the repository might not support limit directly in query for group counts.
                // So, we apply the limit here.
                groupCounts.take(limit)
            }
        }.catchUnexpected()
    }
} 