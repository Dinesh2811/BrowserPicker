package browserpicker.presentation.features.browserpicker.test


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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.presentation.features.browserpicker.*
import browserpicker.presentation.features.browserpicker.PersistentError.Companion.uiErrorState
import browserpicker.presentation.util.BrowserDefault
import dagger.Binds
import dagger.Module
import dagger.hilt.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.*
import kotlin.collections.map


// General App Errors (Assuming some might exist in a common module)
sealed class GenericAppError(override val message: String, override val cause: Throwable? = null) : AppError {
    // data class UnknownError(override val message: String, override val cause: Throwable? = null) : GenericAppError(message, cause) // Already in snippet, assume it exists
    // data class DataIntegrityError(override val message: String, override val cause: Throwable? = null) : GenericAppError(message, cause) // Already in snippet
    data class DataAccessError(override val message: String, override val cause: Throwable? = null) : GenericAppError(message, cause)
}

@Singleton
class UpdateUriUseCase @Inject constructor(
    private val uriParser: UriParser,
    private val hostRuleRepository: HostRuleRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    // No need for InstantProvider here as timestamp is handled by UriHistoryRepositoryImpl
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    // defaultDispatcher and mainDispatcher are not directly used in this flow's logic but kept for consistency if needed later
) {
    operator fun invoke(
        currentBrowserState: BrowserState,
        uri: Uri,
        source: UriSource = UriSource.INTENT,
    ): Flow<BrowserState> = flow {
        // 1. Parse and Validate URI
        val parsedUriResult = uriParser.parseAndValidateWebUri(uri)

        if (parsedUriResult is DomainResult.Failure) {
            val uiError = when (parsedUriResult.error) {
                is UriValidationError.BlankOrEmpty -> TransientError.NULL_OR_EMPTY_URL
                // Consider adding more specific TransientError mappings for other UriValidationErrors
                is UriValidationError.Invalid -> TransientError.INVALID_URL_FORMAT
                else -> TransientError.INVALID_URL_FORMAT // Fallback for other AppErrors from parser
            }
            Timber.w("URI validation failed: ${parsedUriResult.error.message}")
            emit(currentBrowserState.copy(
                uri = uri, // Keep original URI for display if needed
                uriProcessingResult = null,
                uiState = UiState.Error(uiError)
            ))
            return@flow
        }

        val parsedUri = (parsedUriResult as DomainResult.Success<ParsedUri>).data
        var currentUriProcessingResult = UriProcessingResult(
            parsedUri = parsedUri,
            uriSource = source
        )

        // 2. Fetch Host Rule
        when (val hostRuleDomainResult = hostRuleRepository.getHostRuleByHost(parsedUri.host)) {
            is DomainResult.Failure -> {
                Timber.e(hostRuleDomainResult.error.cause, "Failed to fetch host rule for ${parsedUri.host}: ${hostRuleDomainResult.error.message}")
                val uiError: UiError = when (hostRuleDomainResult.error) {
                    is GenericAppError.DataAccessError -> PersistentError.HostRuleAccessFailed(
                        message = "Database error fetching rule for ${parsedUri.host}.", // Provide a user-friendly message
                        cause = hostRuleDomainResult.error.cause
                    )
                    else -> PersistentError.UnknownHostRuleError(
                        message = "Unknown error fetching rule for ${parsedUri.host}.",
                        cause = hostRuleDomainResult.error.cause
                    )
                }
                emit(currentBrowserState.copy(
                    uri = parsedUri.originalUri,
                    uriProcessingResult = currentUriProcessingResult, // Result so far
                    uiState = UiState.Error(uiError)
                ))
                return@flow
            }

            is DomainResult.Success -> {
                val hostRule: HostRule? = hostRuleDomainResult.data
                currentUriProcessingResult = currentUriProcessingResult.copy(hostRule = hostRule)
                if (hostRule != null) {
                    // 3.a. Check if URI is Blocked by rule
                    if (hostRule.uriStatus == UriStatus.BLOCKED) {
                        Timber.i("URI ${parsedUri.originalString} is blocked by rule for host ${hostRule.host}")
                        currentUriProcessingResult = currentUriProcessingResult.copy(isBlocked = true)
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
                            uriProcessingResult = currentUriProcessingResult,
                            uiState = UiState.Blocked
                        ))
                        return@flow
                    }

                    // 3.b. Check for Preferred Browser (if not blocked)
                    if (hostRule.isPreferenceEnabled && !hostRule.preferredBrowserPackage.isNullOrBlank()) {
                        Timber.i("URI ${parsedUri.originalString} to be opened by preference with ${hostRule.preferredBrowserPackage}")
                        currentUriProcessingResult = currentUriProcessingResult.copy(
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
                            uriProcessingResult = currentUriProcessingResult,
                            // UiState.Success can signal the ViewModel to proceed with auto-opening
                            // The actual browser opening logic resides in the Activity/ViewModel.
                            uiState = UiState.Success(Unit)
                        ))
                        return@flow
                    }

                    // 3.c. Check if Bookmarked (if not blocked and no active preference)
                    if (hostRule.uriStatus == UriStatus.BOOKMARKED) {
                        currentUriProcessingResult = currentUriProcessingResult.copy(isBookmarked = true)
                    }
                }

                // 4. No blocking rule, no auto-open preference => Picker will be shown
                Timber.d("URI ${parsedUri.originalString} will be shown in picker. Host rule: $hostRule")
                val newUiState = determineNewUiStateForPicker(currentBrowserState.uiState)

                emit(currentBrowserState.copy(
                    uri = parsedUri.originalUri,
                    uriProcessingResult = currentUriProcessingResult,
                    uiState = newUiState
                ))
            }
        }
    }.flowOn(ioDispatcher) // Perform all operations on IO dispatcher

    private fun determineNewUiStateForPicker(currentUiState: UiState<Unit, UiError>): UiState<Unit, UiError> {
        return when {
            // If previous state was a transient URI error, clear it as we have a valid URI now.
            currentUiState is UiState.Error &&
                    (currentUiState.error == TransientError.NULL_OR_EMPTY_URL ||
                            currentUiState.error == TransientError.INVALID_URL_FORMAT) -> UiState.Idle

            // If previous state was Blocked, but current URI is not, clear it.
            currentUiState is UiState.Blocked -> UiState.Idle

            // If previous state was a success (e.g. from a previous auto-open), reset to Idle for picker.
            currentUiState is UiState.Success -> UiState.Idle

            // Retain other persistent errors (e.g., failed to load browser list) or Idle/Loading.
            currentUiState is UiState.Error<*> -> currentUiState
            currentUiState is UiState.Loading -> currentUiState // Should ideally not be loading during URI update, but handle defensively
            else -> UiState.Idle
        }
    }
}
// endregion


// region: ----------- Presentation Layer: ViewModel -----------
@OptIn(FlowPreview::class)
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    private val instantProvider: InstantProvider, // Keep if other parts of VM use it
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher, // For CPU-bound ops in VM if any
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher, // For launching UC if it didn't specify its own
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher, // For UI updates from VM if needed directly
    private val updateUriUseCase: UpdateUriUseCase,
    // Potentially GetInstalledBrowsersUseCase, LogUserInteractionUseCase etc.
): ViewModel() {
    private val _browserState: MutableStateFlow<BrowserState> = MutableStateFlow(BrowserState(uiState = UiState.Loading))
    val browserState: StateFlow<BrowserState> = _browserState.asStateFlow()

    /**
     * Processes an incoming URI. It will update the browserState based on
     * validation, existing host rules (block, preference), and prepare for
     * picker display if no automatic action is taken.
     *
     * @param uri The URI to process.
     * @param source The source of the URI (Intent, Clipboard, Manual).
     * @param isUriUpdated Callback indicating if the URI processing led to a non-error state update.
     *                     True if the final state in browserState is not a fatal error related to this URI processing,
     *                     false otherwise. Note that UiState.Blocked or UiState.Success(Unit) for auto-open
     *                     are considered successful processing.
     */
    fun updateUri(uri: Uri, source: UriSource = UriSource.INTENT, isUriUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { // Default dispatcher for ViewModel is Main (or MainImmediate)
            // The UseCase itself runs on ioDispatcher as defined in its flowOn operator
            updateUriUseCase(browserState.value, uri, source)
                .catch { e ->
                    Timber.e(e, "Unhandled error in UpdateUriUseCase flow")
                    // Emit a generic error state if the flow itself throws an unhandled exception
                    _browserState.value = browserState.value.copy(
                        uiState = UiState.Error(PersistentError.UnknownHostRuleError("Unexpected error processing URI.", e))
                        // Or a more generic UiError
                    )
                    isUriUpdated(false)
                }
                .collectLatest { newBrowserState ->
                    _browserState.value = newBrowserState
                    val successfullyProcessed = when (newBrowserState.uiState) {
                        is UiState.Error<*> -> {
                            // Check if it's an error that prevents picker display or action
                            // For example, transient URI errors are fatal for this URI.
                            // Persistent errors like "HostRuleAccessFailed" might also be considered fatal for this specific update.
                            !(newBrowserState.uiState.error is TransientError || newBrowserState.uiState.error is PersistentError.HostRuleAccessFailed)
                        }
                        else -> true // Loading, Idle, Success, Blocked are all "processed" states.
                    }
                    isUriUpdated(successfullyProcessed)

                    // TODO: If newBrowserState.uriProcessingResult.autoOpenBrowserPackage is set,
                    // trigger navigation or intent to open that browser with the URI.
                    // This logic would typically be here or in the observing Activity/Composable.
                    // Example:
                    // newBrowserState.uriProcessingResult?.autoOpenBrowserPackage?.let { packageName ->
                    //    newBrowserState.uri?.let { uriToOpen ->
                    //        // send event to UI to open browser with (uriToOpen, packageName)
                    //    }
                    // }
                }
        }
    }

    // Other existing functionalities in your ViewModel...

    // Example: To be called when user selects a browser from picker to open once
    // fun onOpenUriWithSelectedBrowser(browser: BrowserAppInfo) {
    //    viewModelScope.launch {
    //        val currentUriProcessing = browserState.value.uriProcessingResult ?: return@launch
    //        val uriString = currentUriProcessing.parsedUri.originalString
    //        val host = currentUriProcessing.parsedUri.host
    //
    //        // This would ideally be another UseCase: LogUriInteractionUseCase or similar
    //        val result = uriHistoryRepository.addUriRecord(
    //            uriString = uriString,
    //            host = host,
    //            source = currentUriProcessing.uriSource,
    //            action = InteractionAction.OPENED_ONCE,
    //            chosenBrowser = browser.packageName,
    //            associatedHostRuleId = currentUriProcessing.hostRule?.id
    //        )
    //        if (result is DomainResult.Failure) {
    //            Timber.e(result.error.cause, "Failed to log OPENED_ONCE action: ${result.error.message}")
    //            // Handle error, maybe show a toast
    //        }
    //
    //        // Then, send event to UI to open the browser
    //        // _events.emit(OpenBrowserEvent(currentUriProcessing.parsedUri.originalUri, browser.packageName))
    //    }
    // }
}
