package browserpicker.domain.usecase.folders

import browserpicker.domain.model.Folder
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import timber.log.Timber
import javax.inject.Inject

interface DeleteFolderUseCase {
    suspend operator fun invoke(
        folderId: Long,
        // Add recursive flag later if needed: deleteChildren: Boolean = false
        onSuccess: () -> Unit = {},
        onError: (DomainError) -> Unit = {}
    )
}

class DeleteFolderUseCaseImpl @Inject constructor(
    private val repository: FolderRepository
) : DeleteFolderUseCase {
    override suspend fun invoke(
        folderId: Long,
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit
    ) {
        Timber.d("Deleting folder: ID=$folderId")
        // Add validation (cannot delete defaults)
        if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
            onError(DomainError.Validation("Cannot delete default root folders."))
            return
        }
        // Add check for children later if non-recursive delete is default

        repository.deleteFolder(folderId).fold(
            onSuccess = {
                Timber.i("Folder deleted successfully: ID=$folderId")
                onSuccess()
            },
            onFailure = { throwable ->
                Timber.e(throwable, "Failed to delete folder: ID=$folderId")
                onError(throwable.toDomainError("Failed to delete folder."))
                // Could add specific error if it failed due to having children (if that logic is added)
            }
        )
    }
}
