package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.GetRootFoldersUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRootFoldersUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : GetRootFoldersUseCase {
    override operator fun invoke(type: FolderType): Flow<DomainResult<List<Folder>, AppError>> {
        if (type == FolderType.UNKNOWN) {
            // Or return a specific error, depending on desired behavior for UNKNOWN type
            return kotlinx.coroutines.flow.flowOf(DomainResult.Failure(AppError.ValidationError("Cannot get root folders for UNKNOWN type.")))
        }
        return folderRepository.getRootFoldersByType(type)
    }
} 