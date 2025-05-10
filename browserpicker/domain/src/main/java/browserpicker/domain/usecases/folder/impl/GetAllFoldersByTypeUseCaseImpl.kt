package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.GetAllFoldersByTypeUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllFoldersByTypeUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : GetAllFoldersByTypeUseCase {
    override operator fun invoke(type: FolderType): Flow<DomainResult<List<Folder>, AppError>> {
        if (type == FolderType.UNKNOWN) {
            return kotlinx.coroutines.flow.flowOf(DomainResult.Failure(AppError.ValidationError("Cannot get all folders for UNKNOWN type.")))
        }
        return folderRepository.getAllFoldersByType(type)
    }
} 