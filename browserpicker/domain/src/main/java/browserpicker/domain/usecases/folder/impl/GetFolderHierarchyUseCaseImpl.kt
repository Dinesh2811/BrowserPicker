package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.GetFolderHierarchyUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetFolderHierarchyUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : GetFolderHierarchyUseCase {
    override suspend operator fun invoke(folderId: Long): DomainResult<List<Folder>, AppError> {
        val hierarchy = mutableListOf<Folder>()
        var currentFolderId: Long? = folderId

        while (currentFolderId != null) {
            when (val folderResult = folderRepository.getFolder(currentFolderId).first()) {
                is DomainResult.Success -> {
                    val folder = folderResult.data
                    if (folder != null) {
                        hierarchy.add(0, folder) // Add to the beginning to maintain parent-first order
                        currentFolderId = folder.parentFolderId
                    } else {
                        // Should not happen if folderId is valid initially and parent IDs are consistent
                        // If the initial folderId is not found, the first iteration will handle it.
                        if (hierarchy.isEmpty()) { // This means the initial folderId was not found
                            return DomainResult.Failure(AppError.DataNotFound("Folder with ID $folderId not found."))
                        }
                        // This means a parent in the chain was not found, data integrity issue
                        return DomainResult.Failure(AppError.DataIntegrityError("Inconsistent folder hierarchy: parent folder not found during traversal."))
                    }
                }
                is DomainResult.Failure -> {
                    // If the first folder lookup fails, or any parent lookup fails
                    return folderResult
                }
            }
        }
        return DomainResult.Success(hierarchy)
    }
} 