package browserpicker.presentation.features.browserpicker

import android.net.Uri
import browserpicker.domain.model.UriSource
import browserpicker.presentation.util.BrowserDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateUriUseCase @Inject constructor(
    // private val urlValidator: UrlValidator
) {
    operator fun invoke(
        currentBrowserState: BrowserState,
        uri: Uri,
        source: UriSource = UriSource.INTENT,
    ): Flow<BrowserState> = flow {
        val uriString = uri?.toString()?.trim()
        when {
            uriString.isNullOrEmpty() -> {
                emit(currentBrowserState.copy(uiState = UiState.Error(TransientError.NULL_OR_EMPTY_URL)))
                return@flow
            }
            !BrowserDefault.isValidUrl(uriString) -> {
                emit(currentBrowserState.copy(uiState = UiState.Error(TransientError.INVALID_URL_FORMAT)))
                return@flow
            }
            else -> {
                val successState = currentBrowserState.copy(
                    uri = uri,
                    uriSource = source,
                    uriProcessingResult = null,
                    //uiState = if (currentBrowserState.uiState is UiState.Error<*>) { UiState.Idle } else { currentBrowserState.uiState }
                )
                emit(successState)
            }
        }
    }
}
