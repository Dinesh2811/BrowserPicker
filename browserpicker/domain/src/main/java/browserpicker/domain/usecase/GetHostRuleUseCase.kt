package browserpicker.domain.usecase

import browserpicker.core.di.IoDispatcher
import browserpicker.domain.model.*
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface GetHostRuleUseCase {
    operator fun invoke(host: String): Flow<HostRule?>
}

class GetHostRuleUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository
) : GetHostRuleUseCase {
    override fun invoke(host: String): Flow<HostRule?> {
        Timber.d("Getting host rule for: $host")
        return repository.getHostRuleByHost(host)
    }
}


interface GetHostRulesUseCase {
    // Could add filters/sorting later if needed
    operator fun invoke(
        statusFilter: UriStatus? = null,
        folderFilter: Long? = null,
        rootOnlyForStatus: UriStatus? = null
    ): Flow<List<HostRule>>
}

class GetHostRulesUseCaseImpl @Inject constructor(
    private val repository: HostRuleRepository
) : GetHostRulesUseCase {
    override fun invoke(
        statusFilter: UriStatus?,
        folderFilter: Long?,
        rootOnlyForStatus: UriStatus?
    ): Flow<List<HostRule>> {
        Timber.d("Getting host rules with filters: status=$statusFilter, folder=$folderFilter, rootOnly=$rootOnlyForStatus")
        return when {
            rootOnlyForStatus != null -> repository.getRootHostRulesByStatus(rootOnlyForStatus)
            folderFilter != null -> repository.getHostRulesByFolder(folderFilter)
            statusFilter != null -> repository.getHostRulesByStatus(statusFilter)
            else -> repository.getAllHostRules()
        }
    }
}


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
