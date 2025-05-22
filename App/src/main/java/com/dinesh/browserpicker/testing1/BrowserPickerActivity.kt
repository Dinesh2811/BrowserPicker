package com.dinesh.browserpicker.testing1

import android.net.Uri
import androidx.compose.ui.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.activity.compose.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.lifecycle.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.presentation.features.browserpicker.*
import javax.inject.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle // For selected browser
import androidx.compose.material.icons.filled.Error // For error state
import androidx.compose.material.icons.outlined.Info // Placeholder for browser icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

// Placeholder for string resources if not using R.string.*
object BrowserPickerStrings {
    val interceptedUriTitle = "Open With" // Or "Intercepted Link"
    val openOnce = "Open Once"
    val alwaysOpenIn = "Always Open In"
    val bookmarkHost = "Bookmark Host"
    val unbookmarkHost = "Unbookmark Host"
    val blockHost = "Block Host"
    val noBrowsersFound = "No browser apps found on your device."
    val errorLoadingBrowsers = "Could not load browser list."
    val uriBlockedMessage = "This URI is blocked." // Should ideally not show sheet
    val selectBrowserPrompt = "Please select a browser"
    val browserSelected = "Selected:" // Prefix for selected browser name
    val hostBookmarked = "Host bookmarked"
    val hostBookmarkRemoved = "Host bookmark removed"
    val hostBlocked = "Host blocked"
    val settingsSaved = "Preference saved"
}

@Composable
fun rememberAppIcon(packageName: String): Drawable? {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPickerBottomSheetScreen(
    browserViewModel: BrowserPickerViewModel,
    onDismiss: () -> Unit,
) {
    val browserPickerUiState by browserViewModel.browserPickerUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // StandardBottomSheetState is used for BottomSheetScaffold's sheetState
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded,
        skipHiddenState = false // Allow hiding if needed, though typical for picker is not.
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
        snackbarHostState = snackbarHostState
    )
    val scope = rememberCoroutineScope()

    // Effect to handle UI results (Errors, Success messages, navigation/dismissal triggers)
    LaunchedEffect(browserPickerUiState.uiResult) {
        val uiResult = browserPickerUiState.uiResult
        when (uiResult) {
            is UiResult.Error -> {
                when (val error = uiResult.error) {
                    is TransientError -> {
                        snackbarHostState.showSnackbar(
                            message = error.message,
                            duration = SnackbarDuration.Short
                        )
                        browserViewModel.consumeUiOutcome() // Reset transient error
                    }
                    // Persistent errors are handled inline in the UI
                    is PersistentError -> { /* Handled inline */ }

                    is FolderError.DefaultFolderNotFound -> { /* Handled inline */ }
                    is FolderError.FolderAccessFailed -> { /* Handled inline */ }
                }
            }
            is UiResult.Success -> {
                when (uiResult.data) {
                    BrowserPickerUiEffect.AutoOpenBrowser, // This implies the sheet shouldn't be shown or should dismiss
                    is BrowserPickerUiEffect.UriOpenedOnce -> {
                        onDismiss() // URI opened, dismiss the sheet
                    }
                    BrowserPickerUiEffect.SettingsSaved -> {
                        snackbarHostState.showSnackbar(BrowserPickerStrings.settingsSaved, duration = SnackbarDuration.Short)
                        onDismiss() // Preference saved, dismiss
                    }
                    BrowserPickerUiEffect.UriBookmarked -> {
                        snackbarHostState.showSnackbar(BrowserPickerStrings.hostBookmarked, duration = SnackbarDuration.Short)
                    }
                    // Custom logic for Unbookmark if needed (e.g. a different effect from VM)
                    // For now, toggleBookmark handles both, message can be dynamic in VM or a new effect.
                    // BrowserPickerUiEffect.UriUnbookmarked -> {
                    //    snackbarHostState.showSnackbar(BrowserPickerStrings.hostBookmarkRemoved, duration = SnackbarDuration.Short)
                    // }
                    BrowserPickerUiEffect.UriBlocked -> {
                        snackbarHostState.showSnackbar(BrowserPickerStrings.hostBlocked, duration = SnackbarDuration.Short)
                        onDismiss() // URI blocked, dismiss the sheet
                    }
                    BrowserPickerUiEffect.BrowserAppsLoaded -> {
                        // Browsers are loaded, UI will update. No specific snackbar needed.
                    }
                }
                browserViewModel.consumeUiOutcome() // Reset success effect
            }
            UiResult.Blocked -> { // This should ideally prevent sheet from showing
                snackbarHostState.showSnackbar(BrowserPickerStrings.uriBlockedMessage, duration = SnackbarDuration.Long)
                onDismiss()
                browserViewModel.consumeUiOutcome()
            }
            else -> { /* Idle or Loading, handled inline */ }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp, // Fully hidden or fully expanded initially
        sheetSwipeEnabled = true,
        sheetTonalElevation = 2.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // Good M3 color for sheets
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        },
        sheetContent = {
            BrowserPickerSheetContent(
                uiState = browserPickerUiState,
                viewModel = browserViewModel,
                onDismiss = onDismiss // Pass onDismiss for actions that should close the sheet
            )
        }
    ) {
        // Background content, can be empty or a scrim
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)).clickable(enabled = sheetState.isVisible, onClick = onDismiss))
    }
}

@Composable
private fun BrowserPickerSheetContent(
    uiState: BrowserPickerUiState,
    viewModel: BrowserPickerViewModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .navigationBarsPadding() // Respect navigation bars
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) // Padding around content, top padding handled by drag handle
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp)) // Space after drag handle

        // Intercepted URI
        val uriString = uiState.uriProcessingResult?.parsedUri?.originalString ?: "Loading URI..."
        Text(
            text = uriString,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on UiResult
        when (val uiResult = uiState.uiResult) {
            UiResult.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiResult.Error -> {
                // Persistent errors are shown inline
                if (uiResult.error is PersistentError) {
                    ErrorView(
                        message = uiResult.error.message,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
                // Transient errors are handled by Snackbar, so this part might just be empty or a retry
            }
            UiResult.Blocked -> { // Should not happen if logic is correct, but as a fallback
                ErrorView(message = BrowserPickerStrings.uriBlockedMessage, modifier = Modifier.padding(vertical = 32.dp))
            }
            is UiResult.Success, UiResult.Idle -> { // Idle or Success(BrowserAppsLoaded)
                if (uiState.allAvailableBrowsers.isEmpty() && uiState.uiResult != UiResult.Loading) {
                    // This specific case could be due to PersistentError.InstalledBrowserApps.Empty
                    // which is handled above, but as a safeguard:
                    ErrorView(
                        message = BrowserPickerStrings.noBrowsersFound,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    BrowserListAndActions(uiState, viewModel, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun BrowserListAndActions(
    uiState: BrowserPickerUiState,
    viewModel: BrowserPickerViewModel,
    onDismiss: () -> Unit
) {
    // Browser List
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
//            .weight(1f, fill = false) // Takes available space but doesn't push buttons off-screen
            .clip(RoundedCornerShape(12.dp)) // Clip the list for better visuals if it has a background
            .background(MaterialTheme.colorScheme.surfaceContainerLowest) // Slight distinction for list area
    ) {
        items(uiState.allAvailableBrowsers, key = { it.packageName }) { browserApp ->
            BrowserListItem(
                browserAppInfo = browserApp,
                isSelected = browserApp.packageName == uiState.selectedBrowserAppInfo?.packageName,
                onItemClick = { viewModel.onBrowserSelected(browserApp) } // ViewModel handles selection
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Action Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                viewModel.openOnce()
                // onDismiss will be called via LaunchedEffect when UriOpenedOnce effect is received
            },
            enabled = uiState.selectedBrowserAppInfo != null,
            modifier = Modifier.weight(1f)
        ) {
            Text(BrowserPickerStrings.openOnce)
        }

        Button(
            onClick = {
                viewModel.openAlways()
                // onDismiss will be called via LaunchedEffect when SettingsSaved effect is received
            },
            enabled = uiState.selectedBrowserAppInfo != null,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(BrowserPickerStrings.alwaysOpenIn)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Host Actions (Bookmark/Block)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isBookmarked = uiState.uriProcessingResult?.isBookmarked ?: false
        val bookmarkIcon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
        val bookmarkLabel = if (isBookmarked) BrowserPickerStrings.unbookmarkHost else BrowserPickerStrings.bookmarkHost

        TextButton(onClick = { viewModel.toggleBookmark() }) {
            Icon(imageVector = bookmarkIcon, contentDescription = bookmarkLabel, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(bookmarkLabel)
        }

        TextButton(onClick = {
            viewModel.blockHost()
            // onDismiss will be called via LaunchedEffect when UriBlocked effect is received
        }) {
            Icon(imageVector = Icons.Filled.Block, contentDescription = BrowserPickerStrings.blockHost, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(BrowserPickerStrings.blockHost, color = MaterialTheme.colorScheme.error)
        }
    }
    Spacer(modifier = Modifier.height(8.dp)) // Final spacing for bottom
}


@Composable
fun BrowserListItem(
    browserAppInfo: BrowserAppInfo,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    val appIcon = rememberAppIcon(packageName = browserAppInfo.packageName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap().asImageBitmap(),
                    contentDescription = browserAppInfo.appName,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    imageVector = Icons.Outlined.Info, // Placeholder
                    contentDescription = browserAppInfo.appName,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = browserAppInfo.appName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ErrorView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
