package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.toFolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MoveHostRuleToFolderUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository,
    private val folderRepository: FolderRepository
) : MoveHostRuleToFolderUseCase {
    override suspend operator fun invoke(
        hostRuleId: Long,
        destinationFolderId: Long?
    ): DomainResult<Unit, AppError> {
        val hostRuleResult = hostRuleRepository.getHostRuleById(hostRuleId)
        val hostRule = when (hostRuleResult) {
            is DomainResult.Success -> hostRuleResult.data ?: return DomainResult.Failure(
                AppError.DataNotFound("HostRule with ID $hostRuleId not found.")
            )
            is DomainResult.Failure -> return hostRuleResult
        }

        if (hostRule.folderId == destinationFolderId) {
            return DomainResult.Success(Unit) // No change needed
        }

        if (destinationFolderId == null) {
            // Moving out of a folder
            return hostRuleRepository.saveHostRule(
                host = hostRule.host,
                status = hostRule.uriStatus,
                folderId = null, // Key change
                preferredBrowser = hostRule.preferredBrowserPackage,
                isPreferenceEnabled = hostRule.isPreferenceEnabled
            ).mapSuccess { Unit } // We only care about success/failure of the operation, not the returned ID
        }

        // Moving to a new folder
        val folderResult = folderRepository.getFolder(destinationFolderId).first()
        val destinationFolder = when (folderResult) {
            is DomainResult.Success -> folderResult.data ?: return DomainResult.Failure(
                AppError.DataNotFound("Destination folder with ID $destinationFolderId not found.")
            )
            is DomainResult.Failure -> return folderResult
        }

        val requiredFolderType = hostRule.uriStatus.toFolderType()
            ?: return DomainResult.Failure(
                AppError.ValidationError("HostRule with status ${hostRule.uriStatus} cannot be assigned to any folder.")
            )

        if (destinationFolder.type != requiredFolderType) {
            return DomainResult.Failure(
                AppError.ValidationError(
                    "HostRule with status ${hostRule.uriStatus} (requires $requiredFolderType folder) cannot be moved to folder '${destinationFolder.name}' of type ${destinationFolder.type}."
                )
            )
        }

        return hostRuleRepository.saveHostRule(
            host = hostRule.host,
            status = hostRule.uriStatus,
            folderId = destinationFolderId, // Key change
            preferredBrowser = hostRule.preferredBrowserPackage,
            isPreferenceEnabled = hostRule.isPreferenceEnabled
        ).mapSuccess { Unit }
    }
} 