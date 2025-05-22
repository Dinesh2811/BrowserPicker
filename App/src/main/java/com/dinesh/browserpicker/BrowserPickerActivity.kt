package com.dinesh.browserpicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import browserpicker.domain.model.UriSource
import browserpicker.presentation.util.BrowserDefault
import com.dinesh.browserpicker.testing1.BrowserPickerUiEffect
import com.dinesh.browserpicker.testing1.UiResult
import com.dinesh.m3theme.presentation.ThemeViewModel
import com.dinesh.m3theme.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class BrowserPickerActivity: ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    private val viewModel: com.dinesh.browserpicker.testing.BrowserPickerViewModel by viewModels()
    private val browserPickerViewModel: com.dinesh.browserpicker.testing1.BrowserPickerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        splashScreen.setKeepOnScreenCondition {
            themeViewModel.themeUiState.value.isLoading
        }
        enableEdgeToEdge()
//        intent?.data?.let { uri ->
//            processIncomingUri(uri, UriSource.INTENT)
//        }

        setContent {
            val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()

            val systemInDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(systemInDarkTheme) {
                themeViewModel.updateSystemDarkTheme(systemInDarkTheme)
            }

            if (!themeUiState.isLoading) {
                AppTheme(
                    colorScheme = themeUiState.colorScheme,
                    isSystemInDarkTheme = systemInDarkTheme,
                ) {
////                    browserpicker.presentation.test.main.MainScreen()
//                    browserpicker.presentation.features.main.MainScreen()
////                    browserpicker.presentation.features.browserpicker.BrowserPickerScreen()


//                    com.dinesh.browserpicker.testing.BrowserPickerScreen(
//                        viewModel = viewModel,
//                        onBrowserSelected = { packageName, always ->
//                            openSelectedBrowser(packageName, always)
//                        },
//                        onDismiss = { finish() }
//                    )

//                    com.dinesh.browserpicker.testing1.BrowserPickerBottomSheetScreen(
//                        browserViewModel = browserPickerViewModel,
//                        onDismiss = { finish() }
//                    )
                    BrowserPickerAppContent(browserPickerViewModel = browserPickerViewModel)

                    com.dinesh.browserpicker.mock.DebugScreen()
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // This is called when the activity is already running (singleTask launchMode)
        // and a new intent arrives (e.g., another URI is clicked).
        setIntent(intent) // Update the activity's intent
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        processIncomingUri(BrowserDefault.URL.toUri(), UriSource.INTENT)
        handleIntent(intent)
    }

    private fun processIncomingUri(uri: Uri, source: UriSource) {
        viewModel.processUri(uri, source)
    }

    private fun openSelectedBrowser(packageName: String, alwaysUse: Boolean) {
        // This would be handled by the Activity to launch the selected browser
        // with the current URI. The ViewModel already tracks usage stats.
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            // For now, we assume INTENT source when launched via ACTION_VIEW
            browserPickerViewModel.processIncomingUri(uri!!, UriSource.INTENT)
        }
        // Handle other sources (CLIPBOARD, MANUAL) from specific UI actions within the app
    }

    @Composable
    fun BrowserPickerAppContent(
        browserPickerViewModel: com.dinesh.browserpicker.testing1.BrowserPickerViewModel
    ) {
        val uiState by browserPickerViewModel.browserPickerUiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        // State to control sheet visibility
        var showBottomSheet by remember { mutableStateOf(false) }

        // LaunchedEffect to react to ViewModel UiResult changes
        LaunchedEffect(uiState.uiResult) {
            when (val result = uiState.uiResult) {
                UiResult.Loading -> {
                    // Show loading indicator if needed or just wait
                    showBottomSheet = false // Hide sheet while loading
                }
                is UiResult.Success -> {
                    when (result.data) {
                        is BrowserPickerUiEffect.BrowserAppsLoaded -> {
                            // Browsers are loaded, show the bottom sheet
                            showBottomSheet = true
                        }
                        is BrowserPickerUiEffect.AutoOpenBrowser -> {
                            // Auto open browser based on preference
                            val packageName = uiState.uriProcessingResult?.alwaysOpenBrowserPackage
                            val uri = uiState.uriProcessingResult?.parsedUri?.originalUri
                            if (packageName != null && uri != null) {
                                openUriInBrowser(uri, packageName)
                            } else {
                                Toast.makeText(context, "Error: Could not auto-open browser.", Toast.LENGTH_SHORT).show()
                            }
                            showBottomSheet = false // Hide sheet after auto-opening
                            finish() // Finish activity as URI is handled
                        }
                        is BrowserPickerUiEffect.UriOpenedOnce -> {
                            // Open URI once
                            val packageName = result.data.packageName
                            val uri = uiState.uriProcessingResult?.parsedUri?.originalUri
                            if (packageName != null && uri != null) {
                                openUriInBrowser(uri, packageName)
                            } else {
                                Toast.makeText(context, "Error: Could not open browser.", Toast.LENGTH_SHORT).show()
                            }
                            showBottomSheet = false // Hide sheet after opening
                            finish() // Finish activity as URI is handled
                        }
                        is BrowserPickerUiEffect.UriBlocked -> {
                            // URI was blocked, provide feedback and dismiss
                            Toast.makeText(context, "URI blocked: ${uiState.uriProcessingResult?.parsedUri?.host}", Toast.LENGTH_LONG).show()
                            // This might also trigger a system notification if configured in the ViewModel,
                            // but the Activity provides a simple toast here.
                            showBottomSheet = false // Hide sheet
                            finish() // Finish activity as URI is handled
                        }
                        BrowserPickerUiEffect.SettingsSaved -> {
                            // Settings saved, sheet will dismiss based on its internal logic
                            // or we can explicitly dismiss here if it's the final action.
                            // The ViewModel also sends AutoOpenBrowser immediately after SettingsSaved
                            // which will handle the launch and dismissal.
                        }
                        // Add other effects as needed for UI reaction
                        BrowserPickerUiEffect.UriBookmarked -> {}
                    }
                    browserPickerViewModel.consumeUiOutcome() // Important: Consume the effect
                }
                UiResult.Blocked -> {
                    // This state means the host was found to be blocked during initial processing
                    Toast.makeText(context, "This URI is blocked by your rules.", Toast.LENGTH_LONG).show()
                    showBottomSheet = false // Ensure sheet is hidden
                    finish() // Finish activity immediately as URI is blocked
                    browserPickerViewModel.consumeUiOutcome()
                }
                is UiResult.Error -> {
                    // Persistent errors might require a persistent UI element or different handling.
                    // Transient errors are usually handled by the sheet's Snackbar.
                    // If an error occurs that prevents showing the sheet (e.g., no browsers found, invalid URI),
                    // we can show a general toast and finish.
                    Toast.makeText(context, result.error.message, Toast.LENGTH_LONG).show()
                    showBottomSheet = false // Ensure sheet is hidden
                    finish() // Close the activity if we can't show the picker
                    browserPickerViewModel.consumeUiOutcome() // Consume the error
                }
                UiResult.Idle -> {
                    // Nothing to do, awaiting next URI or user interaction
                }
            }
        }

        // Conditionally show the BottomSheetScaffold
//        if (showBottomSheet) {
        com.dinesh.browserpicker.testing1.BrowserPickerBottomSheetScreen(
            browserViewModel = browserPickerViewModel,
            onDismiss = {
                showBottomSheet = false
                finish() // Dismiss the activity if the sheet is dismissed
            }
        )
//        }
    }

    /**
     * Helper function to open a URI in a specific browser.
     */
    private fun openUriInBrowser(uri: Uri, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important for launching from your app
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open browser: ${e.message}", Toast.LENGTH_LONG).show()
            // Log.e("MainActivity", "Failed to open browser $packageName for $uri", e)
        }
    }
}


/*
package com.dinesh.browserpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinesh.m3theme.presentation.ThemeViewModel
import com.dinesh.m3theme.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class BrowserPickerActivity: ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        splashScreen.setKeepOnScreenCondition {
            themeViewModel.themeUiState.value.isLoading
        }
        enableEdgeToEdge()

        setContent {
            val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()

            val systemInDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(systemInDarkTheme) {
                themeViewModel.updateSystemDarkTheme(systemInDarkTheme)
            }

            if (!themeUiState.isLoading) {
                AppTheme(
                    colorScheme = themeUiState.colorScheme,
                    isSystemInDarkTheme = systemInDarkTheme,
                ) {
//                    browserpicker.presentation.test.main.MainScreen()
                    browserpicker.presentation.features.main.MainScreen()
//                    browserpicker.presentation.features.browserpicker.BrowserPickerScreen()
                    com.dinesh.browserpicker.mock.DebugScreen()
                }
            }
        }
    }
}

 */
