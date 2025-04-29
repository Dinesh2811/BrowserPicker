package browserpicker.domain.usecase.history

import browserpicker.core.di.IoDispatcher
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface DeleteUriRecordUseCase {
    suspend operator fun invoke(
        id: Long,
        onSuccess: () -> Unit = {},
        onError: (DomainError) -> Unit = {},
    )
}

class DeleteUriRecordUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeleteUriRecordUseCase {
    override suspend fun invoke(
        id: Long,
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit,
    ) {
        Timber.d("Deleting URI record: ID=$id")
        try {
            val deleted = withContext(ioDispatcher) {
                repository.deleteUriRecord(id)
            }
            if (deleted) {
                Timber.i("URI record deleted successfully: ID=$id")
                onSuccess()
            } else {
                Timber.w("URI record not found or delete failed: ID=$id")
                onError(DomainError.NotFound("UriRecord", id.toString()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete URI record: ID=$id")
            onError(e.toDomainError("Failed to delete history record."))
        }
    }
}