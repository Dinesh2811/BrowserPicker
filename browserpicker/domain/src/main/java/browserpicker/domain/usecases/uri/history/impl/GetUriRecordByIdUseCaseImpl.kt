package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.UriRecord
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.GetUriRecordByIdUseCase
import javax.inject.Inject

class GetUriRecordByIdUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
): GetUriRecordByIdUseCase {
    override suspend operator fun invoke(id: Long): DomainResult<UriRecord?, AppError> {
        return uriHistoryRepository.getUriRecord(id)
    }
}