package browserpicker.domain.usecase.folders

import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import timber.log.Timber
import javax.inject.Inject

interface CreateFolderUseCase {
    suspend operator fun invoke(
        name: String,
        parentFolderId: Long?,
        type: FolderType,
        onSuccess: (Long) -> Unit = {},
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
                Timber.e(throwable.cause, "Failed to create folder")
                throwable.cause?.let { onError(it.toDomainError("Failed to create folder.")) }
            }
        )
    }
}