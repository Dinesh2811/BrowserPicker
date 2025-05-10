package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChildFoldersUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : GetChildFoldersUseCase {
    override operator fun invoke(parentFolderId: Long): Flow<DomainResult<List<Folder>, AppError>> {
        return folderRepository.getChildFolders(parentFolderId)
    }
} 