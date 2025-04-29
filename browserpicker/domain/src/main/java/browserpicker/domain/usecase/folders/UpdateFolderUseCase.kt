package browserpicker.domain.usecase.folders

import browserpicker.domain.model.Folder
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import timber.log.Timber
import javax.inject.Inject

interface UpdateFolderUseCase {
    suspend operator fun invoke(folder: Folder, onSuccess: () -> Unit = {}, onError: (DomainError) -> Unit = {})
}

class UpdateFolderUseCaseImpl @Inject constructor(
    private val repository: FolderRepository
) : UpdateFolderUseCase {
    override suspend fun invoke(
        folder: Folder,
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit
    ) {
        Timber.d("Updating folder: ID=${folder.id}, NewName='${folder.name}', NewParent=${folder.parentFolderId}")
        // Add more validation here if needed (e.g., prevent moving default folders)
        if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
            onError(DomainError.Validation("Cannot modify default root folders."))
            return
        }

        repository.updateFolder(folder).fold(
            onSuccess = {
                Timber.i("Folder updated successfully: ID=${folder.id}")
                onSuccess()
            },
            onFailure = { throwable ->
                Timber.e(throwable, "Failed to update folder: ID=${folder.id}")
                onError(throwable.toDomainError("Failed to update folder."))
            }
        )
    }
}