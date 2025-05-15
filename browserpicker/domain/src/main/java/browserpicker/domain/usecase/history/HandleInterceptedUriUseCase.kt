package browserpicker.domain.usecase.history

import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.core.results.UriValidationError
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.query.HandleUriResult
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.DomainError
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import browserpicker.domain.service.toDomainError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface HandleInterceptedUriUseCase {
    suspend operator fun invoke(uriString: String?, source: UriSource): HandleUriResult
}

class HandleInterceptedUriUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val uriParser: UriParser,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : HandleInterceptedUriUseCase {
    override suspend fun invoke(uriString: String?, source: UriSource): HandleUriResult {
        if (uriString.isNullOrBlank()) {
            Timber.w("Intercepted URI is null or blank.")
            return HandleUriResult.InvalidUri("URI cannot be empty.")
        }

        // Use the injected UriParser to validate and get the parsed model
        val parseResult: DomainResult<ParsedUri?, UriValidationError> = uriParser.parseAndValidateWebUri(uriString)

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
                    hostRule?.getOrNull()?.uriStatus == UriStatus.BLOCKED -> {
                        Timber.i("URI blocked by rule for host: $host")
                        // Record the blocking action immediately
                        // Pass the components from the validated parsedUri
                        uriHistoryRepository.addUriRecord(
                            uriString = parsedUri.originalString,
                            host = parsedUri.host,
                            source = source,
                            action = InteractionAction.BLOCKED_URI_ENFORCED,
                            chosenBrowser = null, // No browser chosen
                            associatedHostRuleId = hostRule.getOrNull()?.id
                        ) // Ignore result for this internal logging
                        HandleUriResult.Blocked
                    }
                    // 2. Preference set and enabled?
                    hostRule?.getOrNull()?.isPreferenceEnabled == true && !hostRule.getOrNull()?.preferredBrowserPackage.isNullOrBlank() && hostRule.getOrNull()?.uriStatus != UriStatus.BLOCKED -> {
                        Timber.i("Opening URI with preference for host: $host, browser: ${hostRule.getOrNull()?.preferredBrowserPackage}")
                        // Record the action
                        // Pass the components from the validated parsedUri
                        uriHistoryRepository.addUriRecord(
                            uriString = parsedUri.originalString,
                            host = parsedUri.host,
                            source = source,
                            action = InteractionAction.OPENED_BY_PREFERENCE,
                            chosenBrowser = hostRule.getOrNull()?.preferredBrowserPackage,
                            associatedHostRuleId = hostRule.getOrNull()?.id
                        ) // Ignore result for this internal logging
                        HandleUriResult.OpenDirectly(hostRule.getOrNull()?.preferredBrowserPackage!!, hostRule.getOrNull()?.id)
                    }
                    // 3. Otherwise, show the picker
                    else -> {
                        Timber.d("No blocking rule or active preference for host: $host. Showing picker.")
                        // Pass the components from the validated parsedUri to the next screen
                        HandleUriResult.ShowPicker(parsedUri.originalString, parsedUri.host, hostRule?.getOrNull()?.id)
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
