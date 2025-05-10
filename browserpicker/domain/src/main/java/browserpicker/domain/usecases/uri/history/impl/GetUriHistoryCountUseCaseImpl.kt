package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.GetUriHistoryCountUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUriHistoryCountUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): GetUriHistoryCountUseCase {
    override operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<Long, AppError>> {
        return uriHistoryRepository.getTotalUriRecordCount(query)
    }
}