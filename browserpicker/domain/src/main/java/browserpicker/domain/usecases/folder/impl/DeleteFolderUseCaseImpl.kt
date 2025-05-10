package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.usecases.folder.DeleteFolderUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteFolderUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository,
    private val hostRuleRepository: HostRuleRepository
) : DeleteFolderUseCase {

    override suspend operator fun invoke(
        folderId: Long,
        forceCascade: Boolean
    ): DomainResult<Unit, AppError> {
        if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
            return DomainResult.Failure(AppError.ValidationError("Cannot delete default root folders."))
        }

        val folderResult = folderRepository.getFolder(folderId).first()
        if (folderResult is DomainResult.Failure) return folderResult
        if (folderResult.getOrNull() == null) {
            return DomainResult.Failure(AppError.DataNotFound("Folder with ID $folderId not found."))
        }

        if (forceCascade) {
            val cascadeResult = deleteContentsRecursively(folderId)
            if (cascadeResult is DomainResult.Failure) {
                return cascadeResult
            }
            // After contents and subfolders are handled, delete the folder itself
            return folderRepository.deleteFolder(folderId)
        } else {
            // Check for child folders
            when (val childFoldersResult = folderRepository.getChildFolders(folderId).first()) {
                is DomainResult.Success -> {
                    if (childFoldersResult.data.isNotEmpty()) {
                        return DomainResult.Failure(
                            AppError.FolderNotEmptyError(
                                folderId,
                                "Folder is not empty. It contains subfolders. Use forceCascade to delete."
                            )
                        )
                    }
                }
                is DomainResult.Failure -> return childFoldersResult
            }

            // Check for associated host rules
            when (val hostRulesResult = hostRuleRepository.getHostRulesByFolder(folderId).first()) {
                is DomainResult.Success -> {
                    if (hostRulesResult.data.isNotEmpty()) {
                        return DomainResult.Failure(
                            AppError.FolderNotEmptyError(
                                folderId,
                                "Folder is not empty. It contains host rules. Use forceCascade to delete."
                            )
                        )
                    }
                }
                is DomainResult.Failure -> return hostRulesResult
            }
            // If not forceCascade and all checks passed (folder is empty)
            return folderRepository.deleteFolder(folderId)
        }
    }

    /**
     * Helper function to recursively delete contents of a folder and its subfolders.
     * This includes clearing host rule associations and deleting child folders.
     */
    private suspend fun deleteContentsRecursively(folderId: Long): DomainResult<Unit, AppError> {
        // 1. Get child folders first to process them recursively (bottom-up approach for deletion)
        val childFoldersResult = folderRepository.getChildFolders(folderId).first()
        val children = when (childFoldersResult) {
            is DomainResult.Success -> childFoldersResult.data
            is DomainResult.Failure -> return childFoldersResult
        }

        // 2. Recursively delete contents and the child folders themselves
        for (child in children) {
            // Delete contents of the child folder first
            val recursiveDeleteResult = deleteContentsRecursively(child.id)
            if (recursiveDeleteResult is DomainResult.Failure) return recursiveDeleteResult
            // Then delete the child folder itself
            val deleteChildFolderResult = folderRepository.deleteFolder(child.id)
            if (deleteChildFolderResult is DomainResult.Failure) return deleteChildFolderResult
        }

        // 3. Clear host rule associations for the current folder (after children are handled)
        val clearRulesResult = hostRuleRepository.clearFolderAssociation(folderId)
        if (clearRulesResult is DomainResult.Failure) return clearRulesResult

        return DomainResult.Success(Unit)
    }
} 