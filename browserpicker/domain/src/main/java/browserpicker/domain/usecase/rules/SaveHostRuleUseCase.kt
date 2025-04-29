package browserpicker.domain.usecase.rules

import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import timber.log.Timber
import javax.inject.Inject

interface SaveHostRuleUseCase {
    suspend operator fun invoke(
        host: String,
        status: UriStatus, // BOOKMARKED, BLOCKED, NONE
        folderId: Long?, // Null for root or if status is NONE
        preferredBrowser: String?, // Only relevant if status != BLOCKED
        isPreferenceEnabled: Boolean, // Only relevant if status != BLOCKED
        onSuccess: (Long) -> Unit = {}, // Callback with rule ID
        onError: (DomainError) -> Unit = {}
    )
}

class SaveHostRuleUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository
) : SaveHostRuleUseCase {
    override suspend fun invoke(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean,
        onSuccess: (Long) -> Unit,
        onError: (DomainError) -> Unit
    ) {
        Timber.d("Saving host rule: Host=$host, Status=$status, Folder=$folderId, PrefBrowser=$preferredBrowser, PrefEnabled=$isPreferenceEnabled")
        if (host.isBlank()) {
            onError(DomainError.Validation("Host cannot be empty."))
            return
        }
        if (status == UriStatus.UNKNOWN) {
            onError(DomainError.Validation("Cannot save rule with UNKNOWN status."))
            return
        }

        repository.saveHostRule(host, status, folderId, preferredBrowser, isPreferenceEnabled).fold(
            onSuccess = { ruleId ->
                Timber.i("Host rule saved successfully: ID=$ruleId")
                onSuccess(ruleId)
            },
            onFailure = { throwable ->
                Timber.e(throwable, "Failed to save host rule for: $host")
                onError(throwable.toDomainError("Failed to save rule."))
            }
        )
    }
}