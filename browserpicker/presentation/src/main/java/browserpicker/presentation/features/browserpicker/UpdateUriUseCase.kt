package browserpicker.presentation.features.browserpicker

import android.net.Uri
import browserpicker.domain.model.UriSource
import browserpicker.presentation.util.BrowserDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

// Assuming BrowserDefault is a utility class or injected dependency for URL validation
// If BrowserDefault requires injection, you'd inject it here.
// For now, assuming it's a static/object utility.

/**
 * UseCase for validating a URI and updating the relevant part of the [BrowserState].
 * It takes the current state as input to produce the next state.
 */
@Singleton
class UpdateUriUseCase @Inject constructor(
    // Inject any dependencies needed for validation, e.g., a URL validator utility
    // private val urlValidator: UrlValidator
) {
    /**
     * Updates the URI within the browser state after validation.
     *
     * Note: This UseCase takes the current state as input to adhere to the pattern
     * where the UseCase emits the complete next state object, similar to
     * GetInstalledBrowserAppsUseCase.
     *
     * @param currentBrowserState The current state of the browser picker.
     * @param uri The new URI to validate and update.
     * @param source The source of the URI update (e.g., Intent, Manual Input).
     * @return A Flow emitting the updated BrowserState.
     */
    operator fun invoke(
        currentBrowserState: BrowserState,
        uri: Uri,
        source: UriSource = UriSource.INTENT
    ): Flow<BrowserState> = flow {
        val uriString = uri.toString()

        if (BrowserDefault.isValidUrl(uriString)) {
            val successState = currentBrowserState.copy(
                uri = uri,
                uriSource = source,
                uriProcessingResult = null,
            )
            emit(successState)
        } else {
            // If invalid, emit a new state reflecting the validation error.
            // Keep the URI fields as they were before the attempt, but set the error state.
            val errorState = currentBrowserState.copy(
                uri = null,
                // uriSource = null,
                uriProcessingResult = null,
                uiState = UiState.Error(TransientError.INVALID_URL_FORMAT) // Set the specific transient error
            )
            emit(errorState)
        }
    }
    // No .onStart, .catch needed here as the operation is synchronous validation
    // and errors are explicitly emitted as part of the state.
}
