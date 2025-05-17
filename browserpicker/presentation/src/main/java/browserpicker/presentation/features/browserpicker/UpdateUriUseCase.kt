package browserpicker.presentation.features.browserpicker

import android.net.Uri
import browserpicker.core.results.UriValidationError
import browserpicker.domain.model.UriSource
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateUriUseCase @Inject constructor(
    private val uriParser: UriParser,
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
