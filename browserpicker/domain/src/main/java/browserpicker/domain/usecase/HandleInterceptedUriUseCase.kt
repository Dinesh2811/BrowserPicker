package browserpicker.domain.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import android.net.Uri // Acceptable in domain for parsing standard URI structure
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.domain.model.*
import browserpicker.domain.model.query.FilterOptions
import browserpicker.domain.model.query.HandleUriResult
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.*
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.PagingDefaults
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri
import browserpicker.domain.service.UriParser

// --- Use Case for handling the initial intercepted URI ---
// package browserpicker.domain.usecase.history

// ... other imports ...

interface HandleInterceptedUriUseCase {
    suspend operator fun invoke(uriString: String?, source: UriSource): HandleUriResult
}

class HandleInterceptedUriUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val uriParser: UriParser, // Inject the parser
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : HandleInterceptedUriUseCase {
    override suspend fun invoke(uriString: String?, source: UriSource): HandleUriResult {
        if (uriString.isNullOrBlank()) {
            Timber.w("Intercepted URI is null or blank.")
            return HandleUriResult.InvalidUri("URI cannot be empty.")
        }

        // Use the injected UriParser to validate and get the parsed model
        val parseResult = uriParser.parseAndValidateWebUri(uriString)

        val parsedUri = parseResult.getOrNull()
        if (parsedUri == null) {
            // Handle invalid/unsupported URI based on parse error
            val error = parseResult.exceptionOrNull()?.toDomainError()?.let {
                // Convert parse errors to user-friendly messages for InvalidUri result
                when (it) {
                    is DomainError.Validation -> it.message
                    else -> "Invalid URI format."
                }
            } ?: "Invalid URI format."

            Timber.w("Invalid or unsupported web URI: $uriString. Reason: $error")
            // Optionally record attempts to open invalid URIs? Maybe later.
            return HandleUriResult.InvalidUri(error)
        }

        // Now use the validated parsedUri
        val host = parsedUri.host

        return withContext(ioDispatcher) {
            try {
                val hostRule = hostRuleRepository.getHostRuleByHost(host).firstOrNull() // Check DB

                when {
                    // 1. Blocked?
                    hostRule?.uriStatus == UriStatus.BLOCKED -> {
                        Timber.i("URI blocked by rule for host: $host")
                        // Record the blocking action immediately
                        // Pass the components from the validated parsedUri
                        uriHistoryRepository.addUriRecord(
                            uriString = parsedUri.originalString,
                            host = parsedUri.host,
                            source = source,
                            action = InteractionAction.BLOCKED_URI_ENFORCED,
                            chosenBrowser = null, // No browser chosen
                            associatedHostRuleId = hostRule.id
                        ) // Ignore result for this internal logging
                        HandleUriResult.Blocked
                    }
                    // 2. Preference set and enabled?
                    hostRule?.isPreferenceEnabled == true && !hostRule.preferredBrowserPackage.isNullOrBlank() && hostRule.uriStatus != UriStatus.BLOCKED -> {
                        Timber.i("Opening URI with preference for host: $host, browser: ${hostRule.preferredBrowserPackage}")
                        // Record the action
                        // Pass the components from the validated parsedUri
                        uriHistoryRepository.addUriRecord(
                            uriString = parsedUri.originalString,
                            host = parsedUri.host,
                            source = source,
                            action = InteractionAction.OPENED_BY_PREFERENCE,
                            chosenBrowser = hostRule.preferredBrowserPackage,
                            associatedHostRuleId = hostRule.id
                        ) // Ignore result for this internal logging
                        HandleUriResult.OpenDirectly(hostRule.preferredBrowserPackage, hostRule.id)
                    }
                    // 3. Otherwise, show the picker
                    else -> {
                        Timber.d("No blocking rule or active preference for host: $host. Showing picker.")
                        // Pass the components from the validated parsedUri to the next screen
                        HandleUriResult.ShowPicker(parsedUri.originalString, parsedUri.host, hostRule?.id)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing intercepted URI for host: $host")
                // Fallback to showing picker on error? Or return an error state?
                // Showing picker might be safest default if DB fails etc.
                // Pass original string/host from parsedUri in case DB lookup failed
                HandleUriResult.ShowPicker(parsedUri.originalString, parsedUri.host, null) // Indicate potential error via null hostRuleId?
            }
        }
    }
}



// --- Use Cases for retrieving history data ---

interface GetPagedUriHistoryUseCase {
    operator fun invoke(
        query: UriHistoryQuery = UriHistoryQuery.DEFAULT,
        pagingConfig: PagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG,
    ): Flow<PagingData<UriRecord>>
}

class GetPagedUriHistoryUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
) : GetPagedUriHistoryUseCase {
    override fun invoke(
        query: UriHistoryQuery,
        pagingConfig: PagingConfig,
    ): Flow<PagingData<UriRecord>> {
        Timber.d("Getting paged URI history with query: $query")
        return repository.getPagedUriRecords(query, pagingConfig)
    }
}

interface GetHistoryOverviewUseCase {
    operator fun invoke(query: UriHistoryQuery = UriHistoryQuery.DEFAULT): Flow<HistoryOverview>
}

data class HistoryOverview(
    val totalCount: Int,
    val groupCounts: List<DomainGroupCount>,
    val dateCounts: List<DomainDateCount>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class GetHistoryOverviewUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
) : GetHistoryOverviewUseCase {
    override fun invoke(query: UriHistoryQuery): Flow<HistoryOverview> {
        Timber.d("Getting history overview with query: $query")
        // Combine multiple flows into one overview object
        return combine(
            repository.getTotalUriRecordCount(query),
            repository.getGroupCounts(query),
            repository.getDateCounts(query)
        ) { total, groups, dates ->
            HistoryOverview(
                totalCount = total,
                groupCounts = groups,
                dateCounts = dates
            )
        }.distinctUntilChanged() // Avoid unnecessary updates if underlying data hasn't changed results
            .catch { e ->
                Timber.e(e, "Error getting history overview")
                emit(HistoryOverview(0, emptyList(), emptyList())) // Emit default on error
            }
    }
}


// --- Use Case for explicitly recording an interaction after picker ---

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
                            .onFailure { Timber.e(it, "Failed to record browser usage for $chosenBrowser after URI interaction.") }
                    }
                }
                onSuccess(recordId)
            },
            onFailure = { throwable ->
                Timber.e(throwable, "Failed to record URI interaction")
                onError(throwable.toDomainError("Failed to save history record."))
            }
        )
    }
}

// --- Use Case for getting filter options ---

interface GetHistoryFilterOptionsUseCase {
    operator fun invoke(): Flow<FilterOptions>
}

@OptIn(ExperimentalCoroutinesApi::class)
class GetHistoryFilterOptionsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
) : GetHistoryFilterOptionsUseCase {
    override fun invoke(): Flow<FilterOptions> {
        Timber.d("Getting filter options...")
        return combine(
            uriHistoryRepository.getDistinctHosts().distinctUntilChanged().catch { emit(emptyList()) },
            hostRuleRepository.getDistinctRuleHosts().distinctUntilChanged().catch { emit(emptyList()) },
            uriHistoryRepository.getDistinctChosenBrowsers().distinctUntilChanged().catch { emit(emptyList()) }
        ) { historyHosts, ruleHosts, browsers ->
            FilterOptions(
                distinctHistoryHosts = historyHosts,
                distinctRuleHosts = ruleHosts,
                distinctChosenBrowsers = browsers
            )
        }
    }
}

// --- Use Case for deleting history ---

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


interface ClearUriHistoryUseCase {
    suspend operator fun invoke(
        onSuccess: () -> Unit = {},
        onError: (DomainError) -> Unit = {},
    )
}

class ClearUriHistoryUseCaseImpl @Inject constructor(
    private val repository: UriHistoryRepository,
) : ClearUriHistoryUseCase {
    override suspend fun invoke(
        onSuccess: () -> Unit,
        onError: (DomainError) -> Unit,
    ) {
        Timber.d("Clearing all URI history...")
        repository.deleteAllUriRecords().fold(
            onSuccess = {
                Timber.i("URI history cleared successfully.")
                onSuccess()
            },
            onFailure = {
                Timber.e(it, "Failed to clear URI history.")
                onError(it.toDomainError("Failed to clear history."))
            }
        )
    }
}
