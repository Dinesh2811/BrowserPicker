package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.analytics.GetTopActionsByHostUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTopActionsByHostUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) : GetTopActionsByHostUseCase {

    override operator fun invoke(host: String): Flow<DomainResult<List<GroupCount>, AppError>> {
        val query = UriHistoryQuery.DEFAULT.copy(
            filterByHost = setOf(host),
            groupBy = UriRecordGroupField.INTERACTION_ACTION
        )
        // The repository's getGroupCounts should provide counts for each InteractionAction for the given host.
        // Sorting by count might be handled by the repository or done client-side if needed.
        return uriHistoryRepository.getGroupCounts(query).catchUnexpected()
    }
} 