package browserpicker.domain.usecase

import browserpicker.domain.repository.FolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import browserpicker.core.di.IoDispatcher
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError

interface InitializeAppDefaultsUseCase {
    /** Ensures essential prerequisites like default folders exist. */
    suspend operator fun invoke(
        onComplete: () -> Unit = {}, // Simple callback for completion
        onError: (DomainError) -> Unit = {}
    )
}

class InitializeAppDefaultsUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InitializeAppDefaultsUseCase {
    override suspend fun invoke(
        onComplete: () -> Unit,
        onError: (DomainError) -> Unit
    ) {
        try {
            Timber.d("Ensuring default folders exist...")
            withContext(ioDispatcher) {
                folderRepository.ensureDefaultFoldersExist()
            }
            Timber.d("Default folder check complete.")
            onComplete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to ensure default folders exist")
            onError(e.toDomainError("Failed to initialize default folders."))
        }
    }
}
