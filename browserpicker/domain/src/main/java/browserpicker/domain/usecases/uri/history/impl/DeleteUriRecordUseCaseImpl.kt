package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.DeleteUriRecordUseCase
import javax.inject.Inject

class DeleteUriRecordUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): DeleteUriRecordUseCase {
    override suspend operator fun invoke(id: Long): DomainResult<Unit, AppError> {
        return uriHistoryRepository.deleteUriRecord(id)
    }
}