package browserpicker.domain.usecase.rules

import browserpicker.core.di.IoDispatcher
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

interface DeleteHostRuleUseCase {
    suspend operator fun invoke(
        host: String, // Delete by host is more user-centric than ID usually
        onSuccess: () -> Unit = {},
        onError: (DomainError) -> Unit = {}
    )
}

class DeleteHostRuleUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DeleteHostRuleUseCase {
    override suspend fun invoke(
        host: String,
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit
    ) {
        Timber.d("Deleting host rule for: $host")
        if (host.isBlank()) {
            onError(DomainError.Validation("Host cannot be empty."))
            return
        }
        repository.deleteHostRuleByHost(host).fold(
            onSuccess = {
                Timber.i("Host rule deleted successfully for: $host")
                onSuccess()
            },
            onFailure = { throwable ->
                Timber.e(throwable, "Failed to delete host rule for: $host")
                // Check if it was just 'not found' vs other error? Repository logs this.
                onError(throwable.toDomainError("Failed to delete rule."))
            }
        )
    }
}
