package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.GetFolderUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFolderUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : GetFolderUseCase {
    override operator fun invoke(folderId: Long): Flow<DomainResult<Folder?, AppError>> {
        return folderRepository.getFolder(folderId)
    }
} 