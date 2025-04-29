package browserpicker.domain.usecase.stats

import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import timber.log.Timber
import javax.inject.Inject

interface ClearBrowserStatsUseCase {
    suspend operator fun invoke(onSuccess: () -> Unit = {}, onError: (DomainError) -> Unit = {})
}

class ClearBrowserStatsUseCaseImpl @Inject constructor(
    private val repository: BrowserStatsRepository,
): ClearBrowserStatsUseCase {
    override suspend fun invoke(
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit,
    ) {
        Timber.d("Clearing all browser stats...")
        repository.deleteAllStats().fold(
            onSuccess = {
                Timber.i("Browser stats cleared successfully.")
                onSuccess()
            },
            onFailure = {
                Timber.e(it, "Failed to clear browser stats.")
                onError(it.toDomainError("Failed to clear browser stats."))
            }
        )
    }
}
