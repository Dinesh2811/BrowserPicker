package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.GetUriHistoryDateCountsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUriHistoryDateCountsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): GetUriHistoryDateCountsUseCase {
    override operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<List<DateCount>, AppError>> {
        return uriHistoryRepository.getDateCounts(query)
    }
}