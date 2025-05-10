package browserpicker.domain.usecases.uri.host.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.usecases.uri.host.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHostRuleUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : GetHostRuleUseCase {
    override fun invoke(host: String): Flow<DomainResult<HostRule?, AppError>> {
        return hostRuleRepository.getHostRuleByHost(host)
    }
}

class GetHostRuleByIdUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : GetHostRuleByIdUseCase {
    override suspend fun invoke(id: Long): DomainResult<HostRule?, AppError> {
        return hostRuleRepository.getHostRuleById(id)
    }
}

class SaveHostRuleUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : SaveHostRuleUseCase {
    override suspend fun invoke(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowserPackage: String?,
        isPreferenceEnabled: Boolean
    ): DomainResult<Long, AppError> {
        if (host.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
        }

        if (status == UriStatus.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("URI status cannot be UNKNOWN"))
        }

        // For NONE status, ensure no folder, browser, or preference is set
        if (status == UriStatus.NONE && (folderId != null || preferredBrowserPackage != null || isPreferenceEnabled)) {
            return DomainResult.Failure(
                AppError.ValidationError("NONE status cannot have folder, preference, or enabled preference")
            )
        }

        // For BLOCKED status, ensure no browser preference is set
        if (status == UriStatus.BLOCKED && (preferredBrowserPackage != null || isPreferenceEnabled)) {
            return DomainResult.Failure(
                AppError.ValidationError("BLOCKED status cannot have a browser preference")
            )
        }

        // Validate browser package is not blank if provided
        if (preferredBrowserPackage != null && preferredBrowserPackage.isBlank()) {
            return DomainResult.Failure(
                AppError.ValidationError("Preferred browser package cannot be blank if provided")
            )
        }

        return hostRuleRepository.saveHostRule(host, status, folderId, preferredBrowserPackage, isPreferenceEnabled)
    }
}

class DeleteHostRuleUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : DeleteHostRuleUseCase {
    override suspend fun invoke(id: Long): DomainResult<Unit, AppError> {
        return hostRuleRepository.deleteHostRuleById(id)
    }
}

class GetAllHostRulesUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : GetAllHostRulesUseCase {
    override fun invoke(): Flow<DomainResult<List<HostRule>, AppError>> {
        return hostRuleRepository.getAllHostRules()
    }
}

class GetHostRulesByStatusUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : GetHostRulesByStatusUseCase {
    override fun invoke(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>> {
        if (status == UriStatus.UNKNOWN) {
            return kotlinx.coroutines.flow.flowOf(
                DomainResult.Failure(AppError.ValidationError("Cannot get host rules with UNKNOWN status"))
            )
        }
        return hostRuleRepository.getHostRulesByStatus(status)
    }
}

class CheckUriStatusUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : CheckUriStatusUseCase {
    override suspend fun invoke(host: String): Flow<DomainResult<UriStatus?, AppError>> {
        if (host.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(
                DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
            )
        }

        return hostRuleRepository.getHostRuleByHost(host).map { result ->
            result.mapSuccess { hostRule -> hostRule?.uriStatus }
        }
    }
}

class GetHostRulesByFolderUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : GetHostRulesByFolderUseCase {
    override fun invoke(folderId: Long): Flow<DomainResult<List<HostRule>, AppError>> {
        if (folderId <= 0) {
            return kotlinx.coroutines.flow.flowOf(
                DomainResult.Failure(AppError.ValidationError("Invalid folder ID"))
            )
        }
        return hostRuleRepository.getHostRulesByFolder(folderId)
    }
}

class ClearHostStatusUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : ClearHostStatusUseCase {
    override suspend fun invoke(host: String): DomainResult<Unit, AppError> {
        if (host.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
        }

        return hostRuleRepository.deleteHostRuleByHost(host)
    }
}

class UpdateHostRuleStatusUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository,
    private val getHostRuleByIdUseCase: GetHostRuleByIdUseCase
) : UpdateHostRuleStatusUseCase {
    override suspend fun invoke(
        hostRuleId: Long,
        newStatus: UriStatus,
        folderId: Long?,
        preferredBrowserPackage: String?,
        isPreferenceEnabled: Boolean
    ): DomainResult<Unit, AppError> {
        if (hostRuleId <= 0) {
            return DomainResult.Failure(AppError.ValidationError("Invalid host rule ID"))
        }

        if (newStatus == UriStatus.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("URI status cannot be UNKNOWN"))
        }

        // Get the existing host rule
        val hostRuleResult = getHostRuleByIdUseCase(hostRuleId)
        if (hostRuleResult is DomainResult.Failure) {
            return DomainResult.Failure(hostRuleResult.error)
        }

        val hostRule = (hostRuleResult as DomainResult.Success).data
            ?: return DomainResult.Failure(AppError.DataNotFound("Host rule not found with ID $hostRuleId"))

        // Apply the same validation rules as in SaveHostRuleUseCase
        if (newStatus == UriStatus.NONE && (folderId != null || preferredBrowserPackage != null || isPreferenceEnabled)) {
            return DomainResult.Failure(
                AppError.ValidationError("NONE status cannot have folder, preference, or enabled preference")
            )
        }

        if (newStatus == UriStatus.BLOCKED && (preferredBrowserPackage != null || isPreferenceEnabled)) {
            return DomainResult.Failure(
                AppError.ValidationError("BLOCKED status cannot have a browser preference")
            )
        }

        if (preferredBrowserPackage != null && preferredBrowserPackage.isBlank()) {
            return DomainResult.Failure(
                AppError.ValidationError("Preferred browser package cannot be blank if provided")
            )
        }

        // Save the host rule with updated status
        val saveResult = hostRuleRepository.saveHostRule(
            host = hostRule.host,
            status = newStatus,
            folderId = folderId,
            preferredBrowser = preferredBrowserPackage,
            isPreferenceEnabled = isPreferenceEnabled
        )

        return when (saveResult) {
            is DomainResult.Success -> DomainResult.Success(Unit)
            is DomainResult.Failure -> DomainResult.Failure(saveResult.error)
        }
    }
}
