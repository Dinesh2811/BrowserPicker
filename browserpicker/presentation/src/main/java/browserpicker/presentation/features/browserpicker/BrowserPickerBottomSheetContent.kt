package browserpicker.presentation.features.browserpicker

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import browserpicker.core.utils.logError
import browserpicker.core.utils.logInfo
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.service.ParsedUri
import browserpicker.presentation.features.browserpicker.uri_info_bar.UriInfoBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserPickerBottomSheetContent(
//    browserViewModel: BrowserViewModel,
    onDismiss: () -> Unit,
) {
//    val browserState: BrowserState by browserViewModel.browserState.collectAsStateWithLifecycle()
//    val filteredBrowsers: List<BrowserAppInfo> by browserViewModel.filteredBrowsers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded), snackbarHostState = snackBarHostState
    )
    var showSecurityDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = sheetState,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        sheetPeekHeight = 500.dp,
        sheetSwipeEnabled = true,
        sheetContent = {
//            AppPickerSheetContent(
//                modifier = Modifier.bottomSheetContentModifier(),
//                uri = browserState.uri,
//                uriProcessingResult = browserState.uriProcessingResult,
//                browserCount = browserState.allAvailableBrowsers.size,
//                searchQuery = browserState.searchQuery,
//                filteredBrowsers = filteredBrowsers,
//                uiState = browserState.uiState,
//                selectedApp = browserState.selectedBrowserAppInfo,
//                onUriEdited = { updatedUri: Uri, callback: (Boolean) -> Unit ->
//                    browserViewModel.updateUri(updatedUri, UriSource.MANUAL, callback)
//                },
//                onBookmarkUri = onBookmarkUri,
//                onBlockUri = onBlockUri,
////                    onSetAlways = { /* TODO: Implement Set Always logic -> Show scope dialog, Call VM */ },
//                onSecurityIconClick = {
//                    browserViewModel.fetchCurrentUriSecurityInfo()
//                    showSecurityDialog = true
//                },
//                onSearchTextChanged = browserViewModel::updateSearchQuery,
//                onRetry = browserViewModel::loadBrowserApps,
//                onAppSelected = browserViewModel::onAppSelected,
//                onJustOnce = { browserViewModel.openUrl(isAlways = false) },
//                onAlways = { browserViewModel.openUrl(isAlways = true) },
//                onUnblockFromBlockedUriState = { onBlockUri() },
//                onDismissFromBlockedUriState = onDismiss
//            )
        },
        sheetTonalElevation = BottomSheetDefaults.Elevation,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = null,
    ) { paddingValues: PaddingValues ->
//        AppPickerBackgroundContent(
//            paddingValues = paddingValues,
//            bottomSheetState = sheetState.bottomSheetState,
//        )
    }
}

data class UriProcessingResult(
//    val requestedUri: String,
    val parsedUri: ParsedUri,
    val uriSource: UriSource,
    val effectivePreference: UriRecord? = null,
    val isBlocked: Boolean = false,
    val alwaysOpenBrowserPackage: String? = null,
    val isBookmarked: Boolean = false,
    val hostRule: HostRule? = null,
)

@Composable
fun ColumnScope.AppPickerSheetContent(
    modifier: Modifier,
    parsedUri: ParsedUri?,
    uriProcessingResult: UriProcessingResult?,
//    browserCount: Int,
//    searchQuery: String,
//    filteredBrowsers: List<BrowserAppInfo>,
//    uiState: UiState<Unit, UiError>,
//    selectedApp: BrowserAppInfo?,
//    onUriEdited: (Uri, (Boolean) -> Unit) -> Unit,
//    onBookmarkUri: () -> Unit,
//    onBlockUri: () -> Unit,
//    onSecurityIconClick: () -> Unit,
//    onSearchTextChanged: (String) -> Unit,
//    onRetry: () -> Unit,
//    onAppSelected: (BrowserAppInfo?) -> Unit,
//    onJustOnce: () -> Unit,
//    onAlways: () -> Unit,
//    onUnblockFromBlockedUriState: () -> Unit,
//    onDismissFromBlockedUriState: () -> Unit,

    onUriEdited: (Uri, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onBookmarkUri: () -> Unit = {},
    onBlockUri: () -> Unit = {},
    onSecurityIconClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        UriInfoBar(
//            uri = uri,
            parsedUri = null,
            uriProcessingResult = uriProcessingResult,
            onUriEdited = { editedUri ->
                onUriEdited(editedUri) { isSuccess ->
                    if (isSuccess) logInfo("URI from EditUriDialog was updated successfully --> $editedUri")
                    else logError("Failed to update URI from EditUriDialog --> $editedUri")
                }
            },
            onBookmarkUri = onBookmarkUri,
            onBlockUri = onBlockUri,
            onSecurityIconClick = onSecurityIconClick,
        )
////                HeaderBar {}
//        AnimatedSearchBar(
//            title = "($browserCount available)",
//            initialSearchText = searchQuery,
//            initialSearchMode = searchQuery.isNotEmpty(),
//            onSearchTextChanged = { searchQuery: String ->
//                onSearchTextChanged(searchQuery.trim())
//            },
//        )
//
//        AnimatedAvailableBrowsers(
//            isVisible = filteredBrowsers.isNotEmpty() && searchQuery.isNotEmpty(),
//            availableFilteredBrowsers = filteredBrowsers.size
//        )
//
//        AnimatedUiContent(
//            state = uiState,
//            filteredBrowsers = filteredBrowsers,
//            searchQuery = searchQuery,
//            selectedApp = selectedApp,
//            uri = uri,
//            onRetry = onRetry,
//            onAppSelected = onAppSelected,
//            onJustOnce = onJustOnce,
//            onAlways = onAlways,
//            onUnblockFromBlockedUriState = onUnblockFromBlockedUriState,
//            onDismissFromBlockedUriState = onDismissFromBlockedUriState,
//        )
    }
}

//sealed interface UiState<out T, out E: UiError> {
//    data object Loading: UiState<Nothing, Nothing>
//    data object Idle: UiState<Nothing, Nothing>
//    data class Success<T>(val data: T): UiState<T, Nothing>
//    data class Error<E: UiError>(val error: E): UiState<Nothing, E>
//    data object Blocked: UiState<Nothing, Nothing>
//}
//
//sealed interface UiError {
//    val message: String
//    val cause: Throwable?
//        get() = null
//}
//
//// Errors that persist until an explicit action (e.g., retry)
//internal sealed interface PersistentError: UiError {
//    data class NoBrowserAppsFound(override val message: String = "No browsers available", override val cause: Throwable? = null): PersistentError
//    data class LoadFailed(override val message: String = "Failed to load browser apps", override val cause: Throwable? = null): PersistentError
//    data class InvalidConfiguration(override val message: String = "Invalid browser configuration", override val cause: Throwable? = null): PersistentError
//    data class UnknownError(override val message: String = "An unknown error occurred", override val cause: Throwable? = null): PersistentError
//}
//
//// Errors that are temporary and should be cleared after being shown (e.g., validation errors)
//internal enum class TransientError(override val message: String): UiError {
//    NULL_OR_EMPTY_URL("URL cannot be empty"),
//    NO_BROWSER_SELECTED("Please select a browser first"),
//    INVALID_URL_FORMAT("Invalid URL format"),
//    LAUNCH_FAILED("Failed to launch browser"),
//    SELECTION_REQUIRED("Please select a browser first")
//}
