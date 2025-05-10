package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.DeleteAllUriHistoryUseCase
import javax.inject.Inject

class DeleteAllUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): DeleteAllUriHistoryUseCase {
    override suspend operator fun invoke(): DomainResult<Int, AppError> {
        return uriHistoryRepository.deleteAllUriRecords()
    }
}