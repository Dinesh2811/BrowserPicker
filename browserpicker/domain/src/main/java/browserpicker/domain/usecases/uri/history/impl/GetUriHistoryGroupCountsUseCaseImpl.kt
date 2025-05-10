package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.GetUriHistoryGroupCountsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUriHistoryGroupCountsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): GetUriHistoryGroupCountsUseCase {
    override operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>> {
        return uriHistoryRepository.getGroupCounts(query)
    }
}
