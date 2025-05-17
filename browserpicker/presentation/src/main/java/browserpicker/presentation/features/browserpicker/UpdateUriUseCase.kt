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
            val errorState = currentBrowserState.copy(uiState = UiState.Error(TransientError.INVALID_URL_FORMAT))
            emit(errorState)
        }
    }
}
