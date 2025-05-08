package browserpicker.domain.usecase.history

import browserpicker.core.di.IoDispatcher
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface RecordUriInteractionUseCase {
    suspend operator fun invoke(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction, // e.g., OPENED_ONCE, DISMISSED
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
        onSuccess: (Long) -> Unit = {}, // Callback with record ID
        onError: (DomainError) -> Unit = {},
    )
}

class RecordUriInteractionUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val browserStatsRepository: BrowserStatsRepository, // To record usage
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecordUriInteractionUseCase {
    override suspend fun invoke(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
        onSuccess: (Long) -> Unit,
        onError: (DomainError) -> Unit,
    ) {
        if (action == InteractionAction.BLOCKED_URI_ENFORCED || action == InteractionAction.OPENED_BY_PREFERENCE || action == InteractionAction.UNKNOWN) {
            Timber.w("Attempted to manually record internal action: $action. Ignoring.")
            onError(DomainError.Validation("Cannot manually record action type: $action"))
            return
        }

        Timber.d("Recording URI interaction: Action=$action, Browser=$chosenBrowser, URI=$uriString")
        val recordResult = uriHistoryRepository.addUriRecord(
            uriString = uriString,
            host = host,
            source = source,
            action = action,
            chosenBrowser = chosenBrowser,
            associatedHostRuleId = associatedHostRuleId
        )

        recordResult.fold(
            onSuccess = { recordId ->
                Timber.i("URI interaction recorded successfully, ID: $recordId")
                // If opened, record browser usage (fire-and-forget, handle errors internally)
                if ((action == InteractionAction.OPENED_ONCE) && !chosenBrowser.isNullOrBlank()) {
                    withContext(ioDispatcher) {
                        browserStatsRepository.recordBrowserUsage(chosenBrowser)
                            .onFailure { Timber.e(it.cause, "Failed to record browser usage for $chosenBrowser after URI interaction.") }
                    }
                }
                onSuccess(recordId)
            },
            onFailure = { throwable ->
                Timber.e(throwable.cause, "Failed to record URI interaction")
                throwable.cause?.let { onError(it.toDomainError("Failed to save history record.")) }
            }
        )
    }
}