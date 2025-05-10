package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.FindFolderByNameAndParentUseCase
import javax.inject.Inject

class FindFolderByNameAndParentUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : FindFolderByNameAndParentUseCase {
    override suspend operator fun invoke(
        name: String,
        parentFolderId: Long?,
        type: FolderType
    ): DomainResult<Folder?, AppError> {
        if (name.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Folder name to find cannot be blank."))
        }
        if (type == FolderType.UNKNOWN) {
            return DomainResult.Failure(AppError.ValidationError("Folder type to find cannot be UNKNOWN."))
        }
        return folderRepository.findFolderByNameAndParent(name, parentFolderId, type)
    }
} 