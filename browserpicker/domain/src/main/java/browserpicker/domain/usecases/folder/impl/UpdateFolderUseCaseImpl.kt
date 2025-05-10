package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject

class UpdateFolderUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : UpdateFolderUseCase {
    override suspend operator fun invoke(folder: Folder): DomainResult<Unit, AppError> {
        if (folder.name.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Folder name cannot be blank."))
        }
        if (folder.type == FolderType.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("Folder type cannot be UNKNOWN."))
        }
        if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
            // For default folders, only allow name changes and ensure type/parent is not changed.
            val existingFolder = when(val res = folderRepository.getFolder(folder.id).first()) {
                is DomainResult.Success -> res.data
                is DomainResult.Failure -> return res
            }
            if (existingFolder == null) return DomainResult.Failure(AppError.DataNotFound("Default folder not found, cannot update."))

            if (existingFolder.type != folder.type || existingFolder.parentFolderId != folder.parentFolderId) {
                return DomainResult.Failure(AppError.ValidationError("Cannot change type or parent of default root folders."))
            }
            // Create an updated folder with only allowed changes (name, updatedAt)
            val updatedFolder = existingFolder.copy(
                name = folder.name,
                updatedAt = Clock.System.now()
            )
            return folderRepository.updateFolder(updatedFolder)
        }

        // Check if folder with this ID exists
        val existingFolderResult = folderRepository.getFolder(folder.id).first()
        val existingFolder = when (existingFolderResult) {
            is DomainResult.Success -> existingFolderResult.data ?: return DomainResult.Failure(
                AppError.DataNotFound("Folder with ID ${folder.id} not found.")
            )
            is DomainResult.Failure -> return existingFolderResult
        }

        // Validate parent folder if it's being changed or set
        if (folder.parentFolderId != null && folder.parentFolderId != existingFolder.parentFolderId) {
            val parentFolderResult = folderRepository.getFolder(folder.parentFolderId).first()
            when (parentFolderResult) {
                is DomainResult.Success -> {
                    val parentFolder = parentFolderResult.data
                    if (parentFolder == null) {
                        return DomainResult.Failure(AppError.DataNotFound("Parent folder with ID ${folder.parentFolderId} not found."))
                    }
                    if (parentFolder.type != folder.type) {
                        return DomainResult.Failure(AppError.ValidationError("Parent folder type (${parentFolder.type}) does not match the folder\'s type (${folder.type})."))
                    }
                    // Prevent making a folder its own parent or child of its descendant (circular dependency)
                    if (parentFolder.id == folder.id) {
                        return DomainResult.Failure(AppError.ValidationError("A folder cannot be its own parent."))
                    }
                    // More complex cycle detection might be needed for deeper hierarchies if allowed by DB directly
                    // For now, basic check.
                }
                is DomainResult.Failure -> return parentFolderResult
            }
        }

        // Check for duplicate name under the same parent and type if name or parent changed
        if (folder.name != existingFolder.name || folder.parentFolderId != existingFolder.parentFolderId || folder.type != existingFolder.type) {
            val duplicateCheckResult = folderRepository.findFolderByNameAndParent(folder.name, folder.parentFolderId, folder.type)
            when (duplicateCheckResult) {
                is DomainResult.Success -> {
                    val duplicate = duplicateCheckResult.data
                    if (duplicate != null && duplicate.id != folder.id) {
                        return DomainResult.Failure(AppError.DataIntegrityError("Another folder with name '${folder.name}' already exists under the specified parent and type."))
                    }
                }
                is DomainResult.Failure -> return duplicateCheckResult
            }
        }

        // Ensure updatedAt is current
        val folderToUpdate = folder.copy(updatedAt = Clock.System.now())
        return folderRepository.updateFolder(folderToUpdate)
    }
} 