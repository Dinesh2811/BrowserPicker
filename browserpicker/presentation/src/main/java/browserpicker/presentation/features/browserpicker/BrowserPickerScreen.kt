package browserpicker.presentation.features.browserpicker

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun BrowserPickerScreen(
//    navController: NavController,
    uriString: String? = null,
) {
    AppPickerBottomSheet {}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppPickerBottomSheet(
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