package browserpicker.domain.usecase.folders

import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface GetFoldersUseCase {
    operator fun invoke(parentFolderId: Long?, type: FolderType): Flow<List<Folder>>
}

class GetFoldersUseCaseImpl @Inject constructor(
    private val repository: FolderRepository
) : GetFoldersUseCase {
    override fun invoke(parentFolderId: Long?, type: FolderType): Flow<List<Folder>> {
        return if (parentFolderId == null) {
            Timber.d("Getting root folders of type: $type")
            repository.getRootFoldersByType(type)
        } else {
            Timber.d("Getting child folders of type $type for parent: $parentFolderId")
            repository.getChildFolders(parentFolderId)
            // Note: Repository should ideally enforce type consistency here,
            // but UI calling this should already know the parent's type.
        }
    }
}
