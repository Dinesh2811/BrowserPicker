package browserpicker.presentation.features.browserpicker

import android.net.Uri
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.UriValidationError
import browserpicker.core.results.onEachFailure
import browserpicker.core.results.onEachSuccess
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class UpdateUriUseCase @Inject constructor(
    private val uriParser: UriParser,
    private val getHostRuleByHostUseCase: GetHostRuleByHostUseCase,
    private val addUriRecordUseCase: AddUriRecordUseCase,
    private val hostRuleRepository: HostRuleRepository,
) {
    operator fun invoke(
        currentBrowserState: BrowserState,
        uri: Uri,
        source: UriSource = UriSource.INTENT,
    ): Flow<BrowserState> = flow {
        uriParser.parseAndValidateWebUri(uri)
            .onFailure {
                val uiErrorState = when(it) {
                    is UriValidationError.BlankOrEmpty -> UiState.Error(TransientError.NULL_OR_EMPTY_URL)
                    else -> UiState.Error(TransientError.INVALID_URL_FORMAT)
                }
                emit(currentBrowserState.copy(uri = null, uriProcessingResult = null, uiState = uiErrorState))
                return@flow
            }.onSuccess { parsedUri: ParsedUri ->
                val uriProcessingResult = UriProcessingResult(parsedUri = parsedUri, uriSource = source)
                with(currentBrowserState) {
                    val uiErrorState = if (uiState is UiState.Error<*> &&
                        (uiState.error == TransientError.NULL_OR_EMPTY_URL || uiState.error == TransientError.INVALID_URL_FORMAT)) {
                        UiState.Idle
                    } else {
                        uiState
                    }

//                    processUri(parsedUri, source, this)

                    val hostRuleFlow: Flow<DomainResult<HostRule?, AppError>> = hostRuleRepository.getHostRuleByHost(parsedUri.host)
                    hostRuleFlow.collect { hostRuleResult: DomainResult<HostRule?, AppError> ->
                        hostRuleResult.onSuccess { hostRule: HostRule? ->
                            emit(currentBrowserState.copy(uri = parsedUri.originalUri, uriProcessingResult = uriProcessingResult, uiState = uiErrorState))
                        }.onFailure { appError: AppError ->
                            emit(currentBrowserState.copy(uri = parsedUri.originalUri, uriProcessingResult = uriProcessingResult, uiState = uiErrorState))
                        }
                    }

                    val hostRuleResult = getHostRuleByHostUseCase(parsedUri.host).first()

                    var activeHostRuleLocal: HostRule? = null
                    var isBlockedByRule = false
                    var autoOpenBrowserPackage: String? = null
                    var isBookmarkedByRule = false
                    var finalUiStateForBrowserState: UiState<Unit, UiError> = UiState.Idle // Default to showing picker

                    when (hostRuleResult) {
                        is DomainResult.Success -> {
                            activeHostRuleLocal = hostRuleResult.data
                            activeHostRuleLocal?.let { rule ->
                                when (rule.uriStatus) {
                                    UriStatus.BLOCKED -> {
                                        isBlockedByRule = true
                                        finalUiStateForBrowserState = UiState.Blocked
                                        addUriRecordUseCase(
                                            uriString = parsedUri.originalString,
                                            host = parsedUri.host,
                                            source = source,
                                            action = InteractionAction.BLOCKED_URI_ENFORCED,
                                            chosenBrowser = null,
                                            associatedHostRuleId = rule.id
                                        ).let { recordResult ->
                                            if (recordResult is DomainResult.Failure) {
                                                System.err.println("Error saving BLOCKED_URI_ENFORCED record: ${recordResult.error.message}")
                                                // Optionally, communicate this failure to UI (e.g. Toast or a secondary error state)
                                            }
                                        }
                                        // If blocked, no further processing for opening browsers or checking bookmarks.
//                                        val processingResult = UriProcessingResult(
//                                            parsedUri = parsedUri,
//                                            uriSource = source,
//                                            activeHostRule = rule,
//                                            isBlockedByRule = true,
//                                            autoOpenBrowserPackage = null,
//                                            isBookmarkedByRule = false // Blocked overrides bookmark
//                                        )
//                                        newBrowserState = newBrowserState.copy(
//                                            uriProcessingResult = processingResult,
//                                            uiState = finalUiStateForBrowserState
//                                        )
//                                        emit(newBrowserState)
                                        return@flow // Stop processing for this URI
                                    }
                                    UriStatus.BOOKMARKED -> {
                                        isBookmarkedByRule = true
                                        // UiState remains Idle unless a preference also applies
                                    }
                                    UriStatus.NONE, UriStatus.UNKNOWN -> {
                                        // No specific status, check for preference
                                    }
                                }

                                // Check for preferred browser if not blocked
                                if (rule.isPreferenceEnabled && !rule.preferredBrowserPackage.isNullOrEmpty()) {
                                    autoOpenBrowserPackage = rule.preferredBrowserPackage
                                    finalUiStateForBrowserState = UiState.Success(Unit) // Signal auto-open
                                    addUriRecordUseCase(
                                        uriString = parsedUri.originalString,
                                        host = parsedUri.host,
                                        source = source,
                                        action = InteractionAction.OPENED_BY_PREFERENCE,
                                        chosenBrowser = autoOpenBrowserPackage,
                                        associatedHostRuleId = rule.id
                                    ).let { recordResult ->
                                        if (recordResult is DomainResult.Failure) {
                                            System.err.println("Error saving OPENED_BY_PREFERENCE record: ${recordResult.error.message}")
                                        }
                                    }
                                }
                            }
                        }
                        is DomainResult.Failure -> {
                            // Log error but proceed as if no rule found. App remains functional.
                            System.err.println("Failed to fetch host rule for ${parsedUri.host}: ${hostRuleResult.error.message}")
                            // finalUiStateForBrowserState remains UiState.Idle
                        }
                    }

                    val successState: BrowserState = this.copy(
                        uri = parsedUri.originalUri,
                        uriProcessingResult = uriProcessingResult,
                        uiState = uiErrorState  // if (this.uiState is UiState.Error<*>) { UiState.Idle } else { this.uiState }
                    )

                    emit(successState)
                }
            }
    }

    private fun processUri(parsedUri: ParsedUri, source: UriSource) {

    }
}

@Singleton
class GetHostRuleByHostUseCase @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) {
    operator fun invoke(host: String): Flow<DomainResult<HostRule?, AppError>> {
        return hostRuleRepository.getHostRuleByHost(host)
    }
}

@Singleton
class AddUriRecordUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
    // InstantProvider is not directly needed here;
    // The repository implementation should handle timestamp generation.
) {
    suspend operator fun invoke(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?
    ): DomainResult<Long, AppError> {
        return uriHistoryRepository.addUriRecord(
            uriString = uriString,
            host = host,
            source = source,
            action = action,
            chosenBrowser = chosenBrowser,
            associatedHostRuleId = associatedHostRuleId
        )
    }
}
