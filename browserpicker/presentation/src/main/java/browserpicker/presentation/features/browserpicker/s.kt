package browserpicker.presentation.features.browserpicker


import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.activity.compose.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.*
import androidx.compose.material.icons.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.navigation.compose.*
import androidx.navigation.*
import androidx.compose.foundation.lazy.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.*
import androidx.compose.foundation.text.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.platform.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
    browserViewModel: BrowserPickerViewModel = hiltViewModel(), // Inject ViewModel
    onDismiss: () -> Unit, // Callback to dismiss the sheet/activity
    onLaunchBrowser: (uri: Uri, packageName: String) -> Unit, // Callback to trigger external browser launch
) {
    // Use collectAsStateWithLifecycle for lifecycle-aware collection
    val browserState: BrowserState by browserViewModel.browserState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded), snackbarHostState = snackBarHostState
    )
    // Assuming snackBarHostState is managed appropriately elsewhere, e.g., in Scaffold
    val scope = rememberCoroutineScope()

    LaunchedEffect(browserState.uiState) {
        when (val uiState = browserState.uiState) {
            is UiState.Blocked -> {
                snackBarHostState.showSnackbar(message = "URL is blocked.", duration = SnackbarDuration.Long)
//                if (sheetState.isVisible) sheetState.partialExpand()
                onDismiss()
            }

            is UiState.Error -> {
                when (val error = uiState.error) {
                    is PersistentError -> { /* Persisted until explicit action */
                        snackBarHostState.showSnackbar(message = error.message, duration = SnackbarDuration.Long)
//                        if (error is PersistentError.LoadFailed || error is PersistentError.NoBrowserAppsFound) {
//                            if (sheetState.bottomSheetState.isVisible) sheetState.bottomSheetState.partialExpand()
//                        }
//                        LogLevel.Error.log("Persistent error occurred: ${error.message} --> ${uiState.error.exception}")
                    }

                    is TransientError -> {
                        Toast.makeText(context, uiState.error.message, Toast.LENGTH_LONG).show()
                        LogLevel.Error.log("Transient error occurred: ${uiState.error.message}")
                        browserViewModel.clearUiState()
                    }
                }
            }

            is UiState.Success -> {
                onDismiss()
                Toast.makeText(context, "Opening ${browserState.selectedBrowserAppInfo?.appName}", Toast.LENGTH_SHORT).show()
                LogLevel.Info.log("${browserState.selectedBrowserAppInfo?.appName} is launched with URI: ${browserState.uri}")
                browserViewModel.clearUiState()
            }

            UiState.Loading, UiState.Idle -> {
                if (!sheetState.bottomSheetState.isVisible) {
                    sheetState.bottomSheetState.expand()
                }
            }
        }
    }
    // LaunchedEffect to react to state changes that require side effects
    LaunchedEffect(browserState.uiState) {
        when (val uiState = browserState.uiState) {
            is UiState.Blocked -> {
                // Side effect: Show message and dismiss UI
                snackBarHostState.showSnackbar(
                    message = "URL is blocked.", duration = SnackbarDuration.Long
                )
                onDismiss()
                browserViewModel.consumeUiOutcome() // <-- Consume the Blocked state after handling
            }

            is UiState.Error -> {
                when (val error = uiState.error) {
                    is PersistentError -> {
                        // Side effect: Show persistent message
                        snackBarHostState.showSnackbar(
                            message = error.message, duration = SnackbarDuration.Long
                        )
                        // Do NOT consume PersistentError here
                    }

                    is TransientError -> {
                        // Side effect: Show transient message
                        Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                        LogLevel.Error.log("Transient error occurred: ${error.message}")
                        browserViewModel.consumeUiOutcome() // <-- Consume the TransientError state after handling
                    }
                }
            }

            is UiState.Success -> {
                when (uiState.data) {
                    BrowserPickerUiEffect.AutoOpenBrowser -> {
                        // Side effect: Launch browser
                        browserState.uri?.let { uriToOpen ->
                            browserState.uriProcessingResult?.alwaysOpenBrowserPackage?.let { packageName ->
                                Timber.i("Launching browser $packageName for ${uriToOpen}")
                                // Use the callback to trigger the launch outside the Composable
                                onLaunchBrowser(uriToOpen, packageName)
                                // Show confirmation message
                                Toast.makeText(context, "Opening via preference...", Toast.LENGTH_SHORT).show()
                                // Dismiss the picker UI
                                onDismiss()
                                browserViewModel.consumeUiOutcome() // <-- Consume the Success state after handling
                            } ?: run {
                                Timber.e("AutoOpenBrowser outcome received but package name is null or URI is null.")
                                // Handle unexpected nulls, maybe show error?
                                // Potentially consume the outcome anyway if it shouldn't stick around
                                browserViewModel.consumeUiOutcome()
                            }
                        } ?: run {
                            Timber.e("AutoOpenBrowser outcome received but URI is null.")
                            // Handle unexpected null URI
                            browserViewModel.consumeUiOutcome()
                        }
                    }
                    null -> {
                        // Should not happen if we only emit Success with a non-null outcome
                        Timber.e("UI State: Received UiState.Success with null data in Composable")
                        // Decide how to handle - maybe consume if it's erroneous but shouldn't block?
                        browserViewModel.consumeUiOutcome() // Defensive consumption
                    }
                    // Handle other BrowserPickerOutcome values if needed in the future
                    else -> TODO()
                }
            }

            UiState.Loading -> {
                // Side effect: Show loading indicator or keep sheet hidden/collapsed
                // sheetState.bottomSheetState.collapse() // Example
            }
            UiState.Idle -> {
                // Side effect: Show the browser picker UI (list of browsers)
                // Ensure the sheet is visible if needed
                // if (!sheetState.bottomSheetState.isVisible) {
                //    sheetState.bottomSheetState.expand()
                // }
            }
        }
    }

    // Rest of your AppPickerBottomSheet Composable rendering code goes here,
    // reacting to the non-UiState fields like browserState.allAvailableBrowsers,
    // browserState.uri, browserState.uiState (for displaying loading, errors, or the picker list)
    // e.g., if (browserState.uiState is UiState.Idle) { /* Display browser list */ }
    // User interactions (like tapping a browser) would call other ViewModel functions,
    // e.g., browserViewModel.onBrowserSelected(selectedBrowser)
}
