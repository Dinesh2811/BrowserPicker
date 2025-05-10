package browserpicker.domain.usecases.uri.host

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.toFolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// Stub implementations for demonstration

class GetHostRuleUseCaseImpl @Inject constructor(): GetHostRuleUseCase {
    override operator fun invoke(host: String): Flow<DomainResult<HostRule?, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetHostRuleByIdUseCaseImpl @Inject constructor(): GetHostRuleByIdUseCase {
    override suspend operator fun invoke(id: Long): DomainResult<HostRule?, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class SaveHostRuleUseCaseImpl @Inject constructor(): SaveHostRuleUseCase {
    override suspend operator fun invoke(
        host: String, 
        status: UriStatus, 
        folderId: Long?, 
        preferredBrowserPackage: String?, 
        isPreferenceEnabled: Boolean
    ): DomainResult<Long, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class DeleteHostRuleUseCaseImpl @Inject constructor(): DeleteHostRuleUseCase {
    override suspend operator fun invoke(id: Long): DomainResult<Unit, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class GetAllHostRulesUseCaseImpl @Inject constructor(): GetAllHostRulesUseCase {
    override operator fun invoke(): Flow<DomainResult<List<HostRule>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetHostRulesByStatusUseCaseImpl @Inject constructor(): GetHostRulesByStatusUseCase {
    override operator fun invoke(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetHostRulesByFolderUseCaseImpl @Inject constructor(): GetHostRulesByFolderUseCase {
    override operator fun invoke(folderId: Long): Flow<DomainResult<List<HostRule>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetRootHostRulesByStatusUseCaseImpl @Inject constructor(): GetRootHostRulesByStatusUseCase {
    override operator fun invoke(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class BookmarkHostUseCaseImpl @Inject constructor(): BookmarkHostUseCase {
    override suspend operator fun invoke(host: String, folderId: Long?): DomainResult<Long, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class BlockHostUseCaseImpl @Inject constructor(): BlockHostUseCase {
    override suspend operator fun invoke(host: String, folderId: Long?): DomainResult<Long, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class ClearHostStatusUseCaseImpl @Inject constructor(): ClearHostStatusUseCase {
    override suspend operator fun invoke(host: String): DomainResult<Unit, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

/*
class RemoveHostRuleFromFolderUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : RemoveHostRuleFromFolderUseCase {

    override suspend fun invoke(hostRuleId: Long): DomainResult<Unit, AppError> {
        // Fetch the host rule by ID
        val getResult = hostRuleRepository.getHostRuleById(hostRuleId)

        return when (getResult) {
            is DomainResult.Success -> {
                val hostRule = getResult.data
                if (hostRule == null) {
                    DomainResult.Failure(AppError.DataNotFound("Host rule with ID $hostRuleId not found."))
                } else {
                    // If a folderId exists, remove it. If not, do nothing.
                    if (hostRule.folderId != null) {
                        // Create a new HostRule instance with folderId set to null
                        val updatedHostRule = hostRule.copy(folderId = null)

                        // Save the updated host rule within the suspend context
                        // Note: saveHostRule identifies by host, not ID. This relies on the repository's saveHostRule handling updates correctly based on existing host.
                        hostRuleRepository.saveHostRule(
                            host = updatedHostRule.host,
                            status = updatedHostRule.uriStatus,
                            folderId = updatedHostRule.folderId,
                            preferredBrowser = updatedHostRule.preferredBrowserPackage,
                            isPreferenceEnabled = updatedHostRule.isPreferenceEnabled
                        ).mapSuccess { Unit } // mapSuccess to convert Long result to Unit
                    } else {
                        // Host rule already not in a folder, return success.
                        DomainResult.Success(Unit)
                    }
                }
            }
            is DomainResult.Failure -> {
                // Pass the failure from the fetch operation
                DomainResult.Failure(getResult.error)
            }
        }
    }
}
 */

class UpdateHostRuleStatusUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository,
    private val folderRepository: FolderRepository // Potentially needed to validate folder existence
) : UpdateHostRuleStatusUseCase {
    override suspend fun invoke(
        hostRuleId: Long,
        newStatus: UriStatus,
        folderId: Long?,
        preferredBrowserPackage: String?,
        isPreferenceEnabled: Boolean
    ): DomainResult<Unit, AppError> {

        // 1. Fetch the host rule
        val hostRuleResult = hostRuleRepository.getHostRuleById(hostRuleId).getOrNull()
        val hostRule = hostRuleResult
        // If getOrNull() is null, hostRuleResult is a Failure or Success(null).
        // Map Success(null) to DataNotFound Failure, propagate other Failures.
        if (hostRule == null) {
            return DomainResult.Failure(AppError.DataNotFound("Host rule with ID $hostRuleId not found."))
        }

        // Apply status precedence logic to determine final values
        val finalFolderId = if (newStatus == UriStatus.BLOCKED) null else folderId
        val finalPreferredBrowserPackage = if (newStatus == UriStatus.BLOCKED) null else preferredBrowserPackage
        val finalIsPreferenceEnabled = if (newStatus == UriStatus.BLOCKED) false else isPreferenceEnabled

        // 2. Validate folder existence if a folderId is provided and status is not BLOCKED
        if (finalFolderId != null) {
            val folderResult = folderRepository.getFolder(finalFolderId).first()
            val folder = folderResult.getOrNull() ?: return folderResult.mapSuccess { Unit }
            // If getOrNull() is null, folderResult is a Failure or Success(null).
            // Map Success(null) to DataNotFound Failure, propagate other Failures.
            if (folder == null) {
                return DomainResult.Failure(AppError.DataNotFound("Folder with ID $finalFolderId not found."))
            }

            // Further validate folder type matches the new status (Bookmark/Block)
            val requiredFolderType = newStatus.toFolderType()
            if (requiredFolderType != null && folder.type != requiredFolderType) {
                return DomainResult.Failure(AppError.ValidationError("Folder type ${folder.type} does not match the required type ${requiredFolderType} for status ${newStatus}."))
            }
        }

        // 3. Create a new HostRule instance with updated properties
        val updatedHostRule = hostRule.copy(
            uriStatus = newStatus,
            folderId = finalFolderId,
            preferredBrowserPackage = finalPreferredBrowserPackage,
            isPreferenceEnabled = finalIsPreferenceEnabled
        )

        // 4. Save the updated host rule
        // saveHostRule identifies by host. It should update the existing rule for that host.
        return hostRuleRepository.saveHostRule(
            host = updatedHostRule.host,
            status = updatedHostRule.uriStatus,
            folderId = updatedHostRule.folderId,
            preferredBrowser = updatedHostRule.preferredBrowserPackage,
            isPreferenceEnabled = updatedHostRule.isPreferenceEnabled
        ).mapSuccess { Unit }
    }
}
