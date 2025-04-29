package browserpicker.domain.usecase.history

import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import timber.log.Timber
import javax.inject.Inject

interface ClearUriHistoryUseCase {
    suspend operator fun invoke(
        onSuccess: () -> Unit = {},
        onError: (DomainError) -> Unit = {},
    )
}

class ClearUriHistoryUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
) : ClearUriHistoryUseCase {
    override suspend fun invoke(
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit,
    ) {
        Timber.d("Clearing all URI history...")
        repository.deleteAllUriRecords().fold(
            onSuccess = {
                Timber.i("URI history cleared successfully.")
                onSuccess()
            },
            onFailure = {
                Timber.e(it, "Failed to clear URI history.")
                onError(it.toDomainError("Failed to clear history."))
            }
        )
    }
}
