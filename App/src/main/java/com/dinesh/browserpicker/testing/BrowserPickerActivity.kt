package com.dinesh.browserpicker.testing

import androidx.activity.ComponentActivity
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
import androidx.compose.material3.Icon
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import browserpicker.domain.service.UriParser
import com.dinesh.m3theme.presentation.ThemeViewModel
import com.dinesh.m3theme.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue
import android.net.Uri
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.ParsedUri
import browserpicker.presentation.features.browserpicker.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.*

//class BrowserPickerActivity : ComponentActivity() {
//    @Inject
//    lateinit var uriParser: UriParser
//
//    private val viewModel: BrowserPickerViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Get URI from intent
//        intent?.data?.let { uri ->
//            processIncomingUri(uri, UriSource.INTENT)
//        }
//
//        setContent {
//            BrowserPickerTheme {
//                BrowserPickerScreen(
//                    viewModel = viewModel,
//                    onBrowserSelected = { packageName, always ->
//                        openSelectedBrowser(packageName, always)
//                    },
//                    onDismiss = { finish() }
//                )
//            }
//        }
//    }
//
//    private fun processIncomingUri(uri: Uri, source: UriSource) {
//        viewModel.processUri(uri, source)
//    }
//
//    private fun openSelectedBrowser(packageName: String, alwaysUse: Boolean) {
//        // This would be handled by the Activity to launch the selected browser
//        // with the current URI. The ViewModel already tracks usage stats.
//        finish()
//    }
//}
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.bottomSheetContentModifier(): Modifier {
    return this
        .fillMaxWidth()
        .fillMaxHeight(0.75f)
        .navigationBarsPadding()
        .imePadding()
//        .imeNestedScroll()
        .padding(horizontal = 16.dp)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserPickerScreen(
    viewModel: BrowserPickerViewModel,
    onBrowserSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.browserPickerUiState.collectAsState()
//    val bottomSheetState = rememberModalBottomSheetState(
////        initialValue = ModalBottomSheetValue.Expanded,
////        skipHalfExpanded = true,
//        skipPartiallyExpanded = true
//    )
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded), snackbarHostState = snackBarHostState
    )
    LaunchedEffect(uiState.uiResult) {
        when (val result = uiState.uiResult) {
            is UiResult.Success -> {
                when (val effect = result.data) {
                    is BrowserPickerUiEffect.AutoOpenBrowser -> {
                        // Auto-open with preferred browser
                        uiState.uriProcessingResult?.alwaysOpenBrowserPackage?.let { packageName ->
                            onBrowserSelected(packageName, true)
                        }
                        viewModel.consumeUiOutcome()
                    }
                    is BrowserPickerUiEffect.UriOpenedOnce -> {
                        onBrowserSelected(effect.packageName, false)
                        viewModel.consumeUiOutcome()
                    }
                    BrowserPickerUiEffect.UriBlocked -> {
                        // Show toast or notification that URI is blocked
                        onDismiss()
                        viewModel.consumeUiOutcome()
                    }
                    BrowserPickerUiEffect.UriBookmarked -> {
                        // Show confirmation that URI was bookmarked
                        viewModel.consumeUiOutcome()
                    }
                    else -> viewModel.consumeUiOutcome()
                }
            }
            UiResult.Blocked -> {
                // Show toast or notification that URI is blocked
                onDismiss()
                viewModel.consumeUiOutcome()
            }
            is UiResult.Error -> {
                // Show error message
                viewModel.consumeUiOutcome()
            }
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissBrowserPicker()
        }
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        sheetPeekHeight = 0.dp,
        sheetSwipeEnabled = true,
        sheetContent = {
            BrowserPickerContent(
                uiState = uiState,
                onBrowserSelected = { browserAppInfo ->
                    viewModel.selectBrowser(browserAppInfo)
                },
                onOpenOnce = {
                    uiState.selectedBrowserAppInfo?.let { browserInfo ->
                        viewModel.openUriWithSelectedBrowser(savePreference = false)
                    }
                },
                onAlwaysOpen = {
                    uiState.selectedBrowserAppInfo?.let { browserInfo ->
                        viewModel.openUriWithSelectedBrowser(savePreference = true)
                    }
                },
                onBookmark = {
                    viewModel.bookmarkCurrentUri()
                },
                onBlock = {
                    viewModel.blockCurrentUri()
                },
                onDismiss = {
                    scope.launch {
                        bottomSheetState.bottomSheetState.partialExpand()
//                        bottomSheetState.bottomSheetState.hide()
                        onDismiss()
                    }
                }
            )
        },
        sheetTonalElevation = 2.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = null,
    ) { paddingValues: PaddingValues ->
        // Background content (dimmed)
        Column {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    scope.launch {
                        bottomSheetState.bottomSheetState.partialExpand()
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
fun BrowserPickerContent(
    uiState: BrowserPickerUiState,
    onBrowserSelected: (BrowserAppInfo) -> Unit,
    onOpenOnce: () -> Unit,
    onAlwaysOpen: () -> Unit,
    onBookmark: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Header with URI info
        uiState.uriProcessingResult?.let { processingResult ->
            UriHeader(processingResult)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading indicator
        when (uiState.uiResult) {
            UiResult.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiResult.Error -> {
                ErrorMessage(error = (uiState.uiResult as UiResult.Error<*>).error)
            }
            else -> {
                // Browser list
                Text(
                    text = "Open with",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.allAvailableBrowsers) { browser ->
                        BrowserItem(
                            browser = browser,
                            isSelected = uiState.selectedBrowserAppInfo?.packageName == browser.packageName,
                            onClick = { onBrowserSelected(browser) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                ActionButtons(
                    isSelected = uiState.selectedBrowserAppInfo != null,
                    isBookmarked = uiState.uriProcessingResult?.isBookmarked == true,
                    onOpenOnce = onOpenOnce,
                    onAlwaysOpen = onAlwaysOpen,
                    onBookmark = onBookmark,
                    onBlock = onBlock,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
fun UriHeader(processingResult: UriProcessingResult) {
    Column {
        Text(
            text = "Opening Link",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = processingResult.parsedUri.host,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = processingResult.parsedUri.originalString,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BrowserItem(
    browser: BrowserAppInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Browser icon placeholder - in a real app, you'd load the app icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = browser.appName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(error: Any) {
    val errorMessage = when (error) {
        is PersistentError.InstalledBrowserApps.Empty -> "No browsers found"
        is PersistentError.InstalledBrowserApps.LoadFailed -> "Failed to load browsers"
        is TransientError -> error.message
        else -> "An unexpected error occurred"
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {


        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActionButtons(
    isSelected: Boolean,
    isBookmarked: Boolean,
    onOpenOnce: () -> Unit,
    onAlwaysOpen: () -> Unit,
    onBookmark: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Main action buttons
        Button(
            onClick = onOpenOnce,
            enabled = isSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Once")
        }

        Button(
            onClick = onAlwaysOpen,
            enabled = isSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Always Open With Selected Browser")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary actions (bookmark/block)
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = if (isBookmarked) onBlock else onBookmark,
                modifier = Modifier.weight(1f)
            ) {
                if (isBookmarked) {
                    Icon(Icons.Rounded.BookmarkRemove, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Bookmark")
                } else {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bookmark")
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onBlock,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Block")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cancel button
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
