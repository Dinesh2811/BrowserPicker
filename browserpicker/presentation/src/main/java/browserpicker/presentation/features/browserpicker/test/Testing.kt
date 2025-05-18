//package browserpicker.presentation.features.browserpicker.test
//
//
//import android.net.Uri
//import browserpicker.core.results.AppError
//import browserpicker.core.results.DomainResult
//import browserpicker.core.results.UriValidationError
//import browserpicker.core.results.onEachFailure
//import browserpicker.core.results.onEachSuccess
//import browserpicker.domain.model.HostRule
//import browserpicker.domain.model.InteractionAction
//import browserpicker.domain.model.UriSource
//import browserpicker.domain.model.UriStatus
//import browserpicker.domain.repository.HostRuleRepository
//import browserpicker.domain.repository.UriHistoryRepository
//import browserpicker.domain.service.ParsedUri
//import browserpicker.domain.service.UriParser
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import javax.inject.Inject
//import javax.inject.Singleton
//import kotlinx.coroutines.flow.first
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.pm.ActivityInfo
//import android.content.pm.PackageInfo
//import android.content.pm.PackageManager
//import android.content.pm.ResolveInfo
//import android.graphics.drawable.Drawable
//import android.os.Build
//import androidx.core.net.toUri
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import browserpicker.core.di.DefaultDispatcher
//import browserpicker.core.di.InstantProvider
//import browserpicker.core.di.IoDispatcher
//import browserpicker.core.di.MainDispatcher
//import browserpicker.core.utils.LogLevel
//import browserpicker.core.utils.log
//import browserpicker.domain.model.BrowserAppInfo
//import browserpicker.presentation.features.browserpicker.*
//import browserpicker.presentation.features.browserpicker.PersistentError.Companion.uiErrorState
//import browserpicker.presentation.util.BrowserDefault
//import dagger.Binds
//import dagger.Module
//import dagger.hilt.*
//import dagger.hilt.android.lifecycle.HiltViewModel
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.FlowPreview
//import kotlinx.coroutines.async
//import kotlinx.coroutines.awaitAll
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.onStart
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import javax.inject.*
//import kotlin.collections.map
//
//
//@OptIn(FlowPreview::class)
//@HiltViewModel
//class BrowserPickerViewModel @Inject constructor(
//    private val instantProvider: InstantProvider,
//    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
//    private val updateUriUseCase: UpdateUriUseCase,
//): ViewModel() {
//    private val _browserState: MutableStateFlow<BrowserState> = MutableStateFlow(BrowserState(uiState = UiState.Loading))
//    val browserState: StateFlow<BrowserState> = _browserState.asStateFlow()
//
//    fun updateUri(uri: Uri, source: UriSource = UriSource.INTENT, isUriUpdated: (Boolean) -> Unit) {
//        // TODO: Functionality that requires attention to process the URI based on my project UseCase and requirements. Implement all the functionality in the 'UpdateUriUseCase' and not in the BrowserPickerViewModel.
//        viewModelScope.launch {
//            val currentState = _browserState.value
//            updateUriUseCase(currentState, uri, source)
//                .collectLatest { newBrowserState: BrowserState ->
//                    _browserState.value = newBrowserState
//                    isUriUpdated(newBrowserState.uiState !is UiState.Error<*>)
//                }
//        }
//    }
//
//    // Other functionalities already implemented. I want you to focus only on 'UpdateUriUseCase' to process the URI
//}
//
//@Singleton
//class GetHostRuleUseCase @Inject constructor(
//    private val hostRuleRepository: HostRuleRepository,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
//) {
//    suspend operator fun invoke(host: String): DomainResult<HostRule?, AppError> {
//        return hostRuleRepository.getHostRuleByHost(host)
//    }
//}
//
//@Singleton
//class ProcessBlockedUriUseCase @Inject constructor(
//    private val uriHistoryRepository: UriHistoryRepository,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//    private val instantProvider: InstantProvider
//) {
//    suspend operator fun invoke(uri: String, host: String, source: UriSource): DomainResult<Long, AppError> {
//        return withContext(ioDispatcher) {
//            uriHistoryRepository.addUriRecord(
//                uriString = uri,
//                host = host,
//                source = source,
//                action = InteractionAction.BLOCKED_URI_ENFORCED,
//                chosenBrowser = null
//            )
//        }
//    }
//}
//
//@Singleton
//class ProcessPreferredBrowserUseCase @Inject constructor(
//    private val uriHistoryRepository: UriHistoryRepository,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//    private val instantProvider: InstantProvider
//) {
//    suspend operator fun invoke(
//        uri: String,
//        host: String,
//        source: UriSource,
//        browserPackage: String
//    ): DomainResult<Long, AppError> {
//        return withContext(ioDispatcher) {
//            uriHistoryRepository.addUriRecord(
//                uriString = uri,
//                host = host,
//                source = source,
//                action = InteractionAction.OPENED_BY_PREFERENCE,
//                chosenBrowser = browserPackage
//            )
//        }
//    }
//}
//
//@Singleton
//class UpdateUriUseCase @Inject constructor(
//    private val instantProvider: InstantProvider,
//    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
//    private val uriParser: UriParser,
//    private val getHostRuleUseCase: GetHostRuleUseCase,
//    private val processBlockedUriUseCase: ProcessBlockedUriUseCase,
//    private val processPreferredBrowserUseCase: ProcessPreferredBrowserUseCase,
//    private val context: Context
//) {
//    operator fun invoke(
//        currentBrowserState: BrowserState,
//        uri: Uri,
//        source: UriSource = UriSource.INTENT,
//    ): Flow<BrowserState> = flow {
//        // Parse and validate the URI first
//        val parseResult = uriParser.parseAndValidateWebUri(uri)
//
//        parseResult.onFailure {
//            val uiErrorState = when(it) {
//                is UriValidationError.BlankOrEmpty -> UiState.Error(TransientError.NULL_OR_EMPTY_URL)
//                else -> UiState.Error(TransientError.INVALID_URL_FORMAT)
//            }
//            emit(currentBrowserState.copy(uri = null, uriProcessingResult = null, uiState = uiErrorState))
//            return@flow
//        }
//
//        val parsedUri = parseResult.getOrNull()
//        val uriString = parsedUri!!.originalUri.toString()
//        val host = parsedUri.host ?: ""
//
//        // Check host rules to determine if URI is blocked or has preferred browser
//        val hostRuleResult = getHostRuleUseCase(host)
//
//        when (hostRuleResult) {
//            is DomainResult.Success -> {
//                val hostRule = hostRuleResult.data
//
//                when (hostRule?.uriStatus) {
//                    UriStatus.BLOCKED -> {
//                        // Process blocked URI
//                        val result = processBlockedUriUseCase(uriString, host, source)
//                        if (result is DomainResult.Success) {
//                            emit(currentBrowserState.copy(
//                                uri = parsedUri.originalUri,
//                                uiState = UiState.Blocked,
//                                uriProcessingResult = UriProcessingResult(parsedUri, source)
//                            ))
//                        } else {
//                            // Handle error
////                            emit(currentBrowserState.copy(
////                                uiState = UiState.Error(PersistentError.UnknownError("Failed to process blocked URI"))
////                            ))
//                        }
//                        return@flow
//                    }
//
//                    UriStatus.BOOKMARKED, null -> {
//                        // Check for preferred browser
//                        hostRule?.takeIf { it.isPreferenceEnabled && !it.preferredBrowserPackage.isNullOrEmpty() }?.let { rule ->
//                            // Has preferred browser, open directly
//                            val result = processPreferredBrowserUseCase(
//                                uriString,
//                                host,
//                                source,
//                                rule.preferredBrowserPackage!!
//                            )
//
//                            if (result is DomainResult.Success) {
//                                // Launch the browser
//                                launchBrowser(rule.preferredBrowserPackage!!, uri)
//
//                                emit(currentBrowserState.copy(
//                                    uri = parsedUri.originalUri,
//                                    selectedBrowserAppInfo = currentBrowserState.allAvailableBrowsers
//                                        .firstOrNull { it.packageName == rule.preferredBrowserPackage },
//                                    uriProcessingResult = UriProcessingResult(parsedUri, source),
//                                    uiState = UiState.Success(Unit)
//                                ))
//                                return@flow
//                            }
//                        }
//                        // No preferred browser or error occurred, show browser picker
//                    }
//
//                    else -> { /* No action needed, will show browser picker */ }
//                }
//            }
//
//            is DomainResult.Failure -> {
//                // Log error but continue to show browser picker
//                Timber.e(hostRuleResult.error.cause, "Error getting host rule for $host")
//            }
//        }
//
//        // Show browser picker if we reach here
//        emit(currentBrowserState.copy(
//            uri = parsedUri.originalUri,
//            uriProcessingResult = UriProcessingResult(parsedUri, source),
//            uiState = if (currentBrowserState.allAvailableBrowsers.isEmpty()) {
//                UiState.Error(PersistentError.InstalledBrowserApps.Empty())
//            } else {
//                UiState.Idle
//            }
//        ))
//    }.flowOn(ioDispatcher)
//
//    private fun launchBrowser(packageName: String, uri: Uri) {
//        try {
//            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
//                `package` = packageName
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            }
//            context.startActivity(intent)
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to launch browser: $packageName")
//        }
//    }
//}
//
