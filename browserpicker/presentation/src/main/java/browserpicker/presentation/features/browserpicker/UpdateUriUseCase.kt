package browserpicker.presentation.features.browserpicker

import android.net.Uri
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.UriValidationError
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

sealed class GenericAppError(override val message: String, override val cause: Throwable? = null) : AppError {
    // data class UnknownError(override val message: String, override val cause: Throwable? = null) : GenericAppError(message, cause) // Already in snippet, assume it exists
    // data class DataIntegrityError(override val message: String, override val cause: Throwable? = null) : GenericAppError(message, cause) // Already in snippet
    data class DataAccessError(override val message: String, override val cause: Throwable? = null) : GenericAppError(message, cause)
}

@Singleton
class UpdateUriUseCase @Inject constructor(
    private val uriParser: UriParser,
    private val getHostRuleByHostUseCase: GetHostRuleByHostUseCase,
    private val addUriRecordUseCase: AddUriRecordUseCase,
    private val hostRuleRepository: HostRuleRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(
        currentBrowserState: BrowserState,
        uri: Uri,
        source: UriSource = UriSource.INTENT,
    ): Flow<BrowserState> = flow {
        // 1. Parse and Validate URI
        when (val parsedUriResult = uriParser.parseAndValidateWebUri(uri)) {
            is DomainResult.Failure -> {
                val uiErrorState = when(parsedUriResult.error) {
                    is UriValidationError.BlankOrEmpty -> UiState.Error(TransientError.NULL_OR_EMPTY_URL)
                    else -> UiState.Error(TransientError.INVALID_URL_FORMAT)
                }
                Timber.w("URI validation failed: ${parsedUriResult.error.message}")
                emit(currentBrowserState.copy(uri = null, uriProcessingResult = null, uiState = uiErrorState))
                return@flow
            }
            is DomainResult.Success -> {
                val parsedUri = parsedUriResult.data
                var uriProcessingResult = UriProcessingResult(parsedUri = parsedUri, uriSource = source)

                // 2. Fetch Host Rule
                when (val hostRuleDomainResult = hostRuleRepository.getHostRuleByHost(parsedUri.host)) {
                    is DomainResult.Failure -> {
                        Timber.e(hostRuleDomainResult.error.cause, "Failed to fetch host rule for ${parsedUri.host}: ${hostRuleDomainResult.error.message}")
                        val uiError: UiError = when (hostRuleDomainResult.error) {
                            is GenericAppError.DataAccessError -> PersistentError.HostRuleAccessFailed(
                                message = "Database error fetching rule for ${parsedUri.host}.",
                                cause = hostRuleDomainResult.error.cause
                            )
                            else -> PersistentError.UnknownHostRuleError(
                                message = "Unknown error fetching rule for ${parsedUri.host}.",
                                cause = hostRuleDomainResult.error.cause
                            )
                        }
                        emit(currentBrowserState.copy(
                            uri = parsedUri.originalUri,
                            uriProcessingResult = uriProcessingResult, // Result so far
                            uiState = UiState.Error(uiError)
                        ))
                        return@flow
                    }

                    is DomainResult.Success -> {
                        val hostRule: HostRule? = hostRuleDomainResult.data
                        uriProcessingResult = uriProcessingResult.copy(hostRule = hostRule)
                        if (hostRule != null) {
                            // 3.a. Check if URI is Blocked by rule
                            if (hostRule.uriStatus == UriStatus.BLOCKED) {
                                Timber.i("URI ${parsedUri.originalString} is blocked by rule for host ${hostRule.host}")
                                uriProcessingResult = uriProcessingResult.copy(isBlocked = true)
                                val historyResult = uriHistoryRepository.addUriRecord(
                                    uriString = parsedUri.originalString,
                                    host = parsedUri.host,
                                    source = source,
                                    action = InteractionAction.BLOCKED_URI_ENFORCED,
                                    chosenBrowser = null,
                                    associatedHostRuleId = hostRule.id
                                )
                                if (historyResult is DomainResult.Failure) {
                                    Timber.e(historyResult.error.cause, "Failed to record blocked URI history: ${historyResult.error.message}")
                                    // Optionally emit a state indicating history recording failure,
                                    // but the primary action (blocking) still proceeds.
                                    // For now, we just log it. A non-critical error state could be added to BrowserState.
                                }
                                emit(currentBrowserState.copy(
                                    uri = parsedUri.originalUri,
                                    uriProcessingResult = uriProcessingResult,
                                    uiState = UiState.Blocked
                                ))
                                return@flow
                            }

                            // 3.b. Check for Preferred Browser (if not blocked)
                            if (hostRule.isPreferenceEnabled && !hostRule.preferredBrowserPackage.isNullOrBlank()) {
                                Timber.i("URI ${parsedUri.originalString} to be opened by preference with ${hostRule.preferredBrowserPackage}")
                                uriProcessingResult = uriProcessingResult.copy(
                                    alwaysOpenBrowserPackage = hostRule.preferredBrowserPackage
                                )
                                val historyResult = uriHistoryRepository.addUriRecord(
                                    uriString = parsedUri.originalString,
                                    host = parsedUri.host,
                                    source = source,
                                    action = InteractionAction.OPENED_BY_PREFERENCE,
                                    chosenBrowser = hostRule.preferredBrowserPackage,
                                    associatedHostRuleId = hostRule.id
                                )
                                if (historyResult is DomainResult.Failure) {
                                    Timber.e(historyResult.error.cause, "Failed to record preference URI history: ${historyResult.error.message}")
                                    // Log, similar to blocked URI history failure.
                                }
                                emit(currentBrowserState.copy(
                                    uri = parsedUri.originalUri,
                                    uriProcessingResult = uriProcessingResult,
                                    uiState = UiState.Success(BrowserPickerUiEffect.AutoOpenBrowser)
                                ))
                                return@flow
                            }

                            // 3.c. Check if Bookmarked (if not blocked and no active preference)
                            if (hostRule.uriStatus == UriStatus.BOOKMARKED) {
                                uriProcessingResult = uriProcessingResult.copy(isBookmarked = true)
                            }
                        }

                        // 4. No blocking rule, no auto-open preference => Picker will be shown
                        Timber.d("URI ${parsedUri.originalString} will be shown in picker. Host rule: $hostRule")
                        val newUiState = determineNewUiStateForPicker(currentBrowserState.uiState)

                        emit(currentBrowserState.copy(
                            uri = parsedUri.originalUri,
                            uriProcessingResult = uriProcessingResult,
                            uiState = newUiState
                        ))
                    }
                }


                val uiErrorState = if (currentBrowserState.uiState is UiState.Error<*> &&
                    (currentBrowserState.uiState.error == TransientError.NULL_OR_EMPTY_URL || currentBrowserState.uiState.error == TransientError.INVALID_URL_FORMAT)
                ) {
                    UiState.Idle
                } else {
                    currentBrowserState.uiState
                }
                val successState: BrowserState = currentBrowserState.copy(
                    uri = parsedUri.originalUri,
                    uriProcessingResult = uriProcessingResult,
                    uiState = uiErrorState  // if (this.uiState is UiState.Error<*>) { UiState.Idle } else { this.uiState }
                )

                emit(successState)
            }
        }
    }.flowOn(ioDispatcher)

    private fun determineNewUiStateForPicker(currentUiState: UiState<BrowserPickerUiEffect, UiError>): UiState<BrowserPickerUiEffect, UiError> {
        return when {
            // If previous state was a transient URI error, clear it as we have a valid URI now.
            currentUiState is UiState.Error &&
                    (currentUiState.error == TransientError.NULL_OR_EMPTY_URL ||
                            currentUiState.error == TransientError.INVALID_URL_FORMAT) -> UiState.Idle

            // If previous state was Blocked, but current URI is not, clear it.
            currentUiState is UiState.Blocked -> UiState.Idle

            // If previous state was a success (e.g. from a previous auto-open), reset to Idle for picker.
//            currentUiState is UiState.Success -> UiState.Idle

            currentUiState is UiState.Success && currentUiState.data == BrowserPickerUiEffect.AutoOpenBrowser -> UiState.Idle
            // Retain other persistent errors (e.g., failed to load browser list) or Idle/Loading.
            currentUiState is UiState.Error -> currentUiState
            currentUiState is UiState.Loading -> currentUiState // Should ideally not be loading during URI update, but handle defensively
            else -> UiState.Idle
        }
    }

    private fun processUri(parsedUri: ParsedUri, source: UriSource, currentBrowserState: BrowserState) {

    }
}

@Singleton
class GetHostRuleByHostUseCase @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) {
    suspend operator fun invoke(host: String): DomainResult<HostRule?, AppError> {
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

/*

        uriParser.parseAndValidateWebUri(uri)
            .onFailure {
                val uiErrorState = when(it) {
                    is UriValidationError.BlankOrEmpty -> UiState.Error(TransientError.NULL_OR_EMPTY_URL)
                    else -> UiState.Error(TransientError.INVALID_URL_FORMAT)
                }
                Timber.w("URI validation failed: ${it.message}")
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

                    val hostRuleResult: DomainResult<HostRule?, AppError> = hostRuleRepository.getHostRuleByHost(parsedUri.host)
                    hostRuleResult.onSuccess { hostRule: HostRule? ->
                        emit(currentBrowserState.copy(uri = parsedUri.originalUri, uriProcessingResult = uriProcessingResult, uiState = uiErrorState))
                    }.onFailure { appError: AppError ->
                        emit(currentBrowserState.copy(uri = parsedUri.originalUri, uriProcessingResult = uriProcessingResult, uiState = uiErrorState))
                    }



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

 */