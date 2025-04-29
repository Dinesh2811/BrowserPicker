package browserpicker.domain.usecase.folders

import browserpicker.domain.model.*
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface GetFoldersUseCase {
    operator fun invoke(
        parentFolderId: Long?, // Null for root folders
        type: FolderType
    ): Flow<List<Folder>>
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


interface CreateFolderUseCase {
    suspend operator fun invoke(
        name: String,
        parentFolderId: Long?,
        type: FolderType,
        onSuccess: (Long) -> Unit = {}, // Callback with folder ID
        onError: (DomainError) -> Unit = {}
    )
}

class CreateFolderUseCaseImpl @Inject constructor(
    private val repository: FolderRepository
) : CreateFolderUseCase {
    override suspend fun invoke(
        name: String,
        parentFolderId: Long?,
        type: FolderType,
        onSuccess: (Long) -> Unit,
        onError: (DomainError) -> Unit
    ) {
        Timber.d("Creating folder: Name='$name', Parent=$parentFolderId, Type=$type")
        repository.createFolder(name, parentFolderId, type).fold(
            onSuccess = { folderId ->
                Timber.i("Folder created successfully: ID=$folderId")
                onSuccess(folderId)
            },
            onFailure = { throwable ->
                Timber.e(throwable, "Failed to create folder")
                onError(throwable.toDomainError("Failed to create folder."))
            }
        )
    }
}


interface UpdateFolderUseCase {
    suspend operator fun invoke(
        folder: Folder, // Pass the entire updated object (ID must be valid)
        onSuccess: () -> Unit = {},
        onError: (DomainError) -> Unit = {}
    )
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
