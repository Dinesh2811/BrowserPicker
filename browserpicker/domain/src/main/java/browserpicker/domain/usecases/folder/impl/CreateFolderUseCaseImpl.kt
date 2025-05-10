package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.CreateFolderUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateFolderUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : CreateFolderUseCase {
    override suspend operator fun invoke(
        name: String,
        parentFolderId: Long?,
        type: FolderType
    ): DomainResult<Long, AppError> {
        if (name.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Folder name cannot be blank."))
        }
        if (type == FolderType.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("Folder type cannot be UNKNOWN."))
        }

        if (parentFolderId != null) {
            when (val parentFolderResult = folderRepository.getFolder(parentFolderId).first()) {
                is DomainResult.Success -> {
                    val parentFolder = parentFolderResult.data
                    if (parentFolder == null) {
                        return DomainResult.Failure(AppError.DataNotFound("Parent folder with ID $parentFolderId not found."))
                    }
                    if (parentFolder.type != type) {
                        return DomainResult.Failure(AppError.ValidationError("Parent folder type (${parentFolder.type}) does not match the new folder\'s type ($type)."))
                    }
                }
                is DomainResult.Failure -> return parentFolderResult // Propagate error
            }
        }

        when (val findResult = folderRepository.findFolderByNameAndParent(name, parentFolderId, type)) {
            is DomainResult.Success -> {
                if (findResult.data != null) {
                    return DomainResult.Failure(AppError.DataIntegrityError("Folder with name '$name' already exists under the specified parent and type."))
                }
            }
            is DomainResult.Failure -> return findResult // Propagate error
        }

        return folderRepository.createFolder(name, parentFolderId, type)
    }
} 