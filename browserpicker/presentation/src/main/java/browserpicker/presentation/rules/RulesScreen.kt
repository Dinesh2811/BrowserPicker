package browserpicker.presentation.rules

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import browserpicker.domain.model.*
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.picker.BrowserAppInfo
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RulesScreen(
    viewModel: RulesViewModel = hiltViewModel(),
    initialType: UriStatus, // Received from navigation args
    availableBrowsers: List<BrowserAppInfo> // Received from MainScreen/Activity
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() } // Screen-specific messages

    // Handle ViewModel messages
    LaunchedEffect(state.userMessages) {
        state.userMessages.firstOrNull()?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.message,
                duration = SnackbarDuration.Short,
                // TODO: Add action button if needed
            )
            // After message is shown, clear it from ViewModel
            viewModel.clearMessage(message.id)
        }
    }

    // Set initial type from navigation argument only once
    LaunchedEffect(initialType) {
        if (state.currentType != initialType) {
            viewModel.setFilterType(initialType)
        }
    }

    // State for folder filter dropdown menu
    var showFolderFilterDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${state.currentType.name.capitalize()} Rules")
                }
                // No actions in top app bar for now, FAB is for Add
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Use screen's snackbar
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddRuleDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // --- Rule Type Tabs ---
            TabRow(selectedTabIndex = if (state.currentType == UriStatus.BOOKMARKED) 0 else 1) {
                Tab(
                    selected = state.currentType == UriStatus.BOOKMARKED,
                    onClick = { viewModel.setFilterType(UriStatus.BOOKMARKED) },
                    text = { Text("Bookmarks") }
                )
                Tab(
                    selected = state.currentType == UriStatus.BLOCKED,
                    onClick = { viewModel.setFilterType(UriStatus.BLOCKED) },
                    text = { Text("Blocked") }
                )
            }

            // --- Folder Filter ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFolderFilterDropdown = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Filter by Folder:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = state.folders.find { it.id == state.selectedFolderId }?.name
                        ?: if (state.selectedFolderId == null) "All ${state.currentType.name}" else "Unknown Folder",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Folder")

                DropdownMenu(
                    expanded = showFolderFilterDropdown,
                    onDismissRequest = { showFolderFilterDropdown = false }
                ) {
                    // Option for 'All'
                    DropdownMenuItem(
                        text = { Text("All ${state.currentType.name}") },
                        onClick = {
                            viewModel.setSelectedFolder(null)
                            showFolderFilterDropdown = false
                        }
                    )
                    Divider()
                    // List available folders
                    state.folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = {
                                viewModel.setSelectedFolder(folder.id)
                                showFolderFilterDropdown = false
                            }
                        )
                    }
                }
            }

            Divider() // Separator below filter

            // --- Rules List ---
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No ${state.currentType.name.lowercase()} rules found.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rules, key = { it.host }) { rule ->
                        RuleItem(
                            rule = rule,
                            onEditClick = { viewModel.showEditRuleDialog(rule) },
                            onDeleteClick = { viewModel.deleteRule(rule.host) }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // --- Rule Add/Edit Dialog ---
    when (val dialogState = state.dialogState) {
        RuleDialogState.Hidden -> Unit // Do nothing
        is RuleDialogState.Add -> {
            RuleDialog(
                title = "Add ${dialogState.currentType.name.capitalize()} Rule",
                initialHost = dialogState.host,
                initialStatus = dialogState.currentType,
                initialFolderId = null,
                initialPreferredBrowser = null,
                initialPreferenceEnabled = true,
                availableFolders = dialogState.availableFolders,
                availableBrowsers = availableBrowsers, // Pass browsers to dialog
                onSave = { host, status, folderId, preferredBrowser, isPreferenceEnabled ->
                    viewModel.saveRule(host, status, folderId, preferredBrowser, isPreferenceEnabled)
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is RuleDialogState.Edit -> {
            RuleDialog(
                title = "Edit Rule",
                initialHost = dialogState.rule.host,
                initialStatus = dialogState.rule.uriStatus,
                initialFolderId = dialogState.rule.folderId,
                initialPreferredBrowser = dialogState.rule.preferredBrowserPackage,
                initialPreferenceEnabled = dialogState.rule.isPreferenceEnabled,
                availableFolders = dialogState.availableFolders,
                availableBrowsers = availableBrowsers, // Pass browsers to dialog
                onSave = { host, status, folderId, preferredBrowser, isPreferenceEnabled ->
                    // Pass the rule ID to the ViewModel if needed for update logic,
                    // but saveRule in VM uses host as key, so ID might be internal.
                    viewModel.saveRule(host, status, folderId, preferredBrowser, isPreferenceEnabled)
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
    }
}

@Composable
fun RuleItem(
    rule: HostRule,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(rule.host, style = MaterialTheme.typography.titleMedium)
            // Show status if not implicit from list (e.g., combined list)
            // if (rule.uriStatus != state.currentType) { Text("Status: ${rule.uriStatus.name}") }

            // Show folder if assigned
            if (rule.folderId != null) {
                // TODO: Resolve folder name from state.folders list or fetch separately?
                // Simple for now: just show ID or a placeholder
                Text("Folder ID: ${rule.folderId}", style = MaterialTheme.typography.bodySmall)
            }

            // Show preference details if applicable and enabled
            if (rule.uriStatus != UriStatus.BLOCKED && !rule.preferredBrowserPackage.isNullOrBlank() && rule.isPreferenceEnabled) {
                Text("Prefer: ${rule.preferredBrowserPackage}", style = MaterialTheme.typography.bodySmall)
            } else if (rule.uriStatus != UriStatus.BLOCKED && rule.preferredBrowserPackage.isNullOrBlank() && rule.isPreferenceEnabled) {
                Text("Preference enabled but no browser set", style = MaterialTheme.typography.bodySmall)
            } else if (rule.uriStatus != UriStatus.BLOCKED && rule.preferredBrowserPackage != null && !rule.isPreferenceEnabled) {
                Text("Preference disabled for: ${rule.preferredBrowserPackage}", style = MaterialTheme.typography.bodySmall)
            }

        }
        Row {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Rule")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Rule")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleDialog(
    title: String,
    initialHost: String,
    initialStatus: UriStatus, // Status is generally fixed by which tab/list triggered dialog
    initialFolderId: Long?,
    initialPreferredBrowser: String?,
    initialPreferenceEnabled: Boolean,
    availableFolders: List<Folder>, // Folders matching the rule type (Bookmarks/Blocked)
    availableBrowsers: List<BrowserAppInfo>, // List of installed browsers
    onSave: (host: String, status: UriStatus, folderId: Long?, preferredBrowser: String?, isPreferenceEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // State for dialog inputs
    var hostText by remember { mutableStateOf(initialHost) }
    var folderIdState by remember { mutableStateOf(initialFolderId) }
    var preferredBrowserText by remember { mutableStateOf(initialPreferredBrowser ?: "") }
    var isPreferenceEnabledState by remember { mutableStateOf(initialPreferenceEnabled) }

    // State for folder dropdown
    var showFolderDropdown by remember { mutableStateOf(false) }

    // State for preferred browser dropdown (if using picker)
    var showBrowserDropdown by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                // Host Input (Disable editing if not adding)
                OutlinedTextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = initialHost.isNotBlank() && title != "Add ${initialStatus.name.capitalize()} Rule" // Disable editing host if not adding
                )
                Spacer(Modifier.height(8.dp))

                // Status Display (Not editable in dialog if fixed by list)
                Text("Status: ${initialStatus.name}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                // Folder Selector (Only if status is BOOKMARKED or BLOCKED)
                if (initialStatus != UriStatus.NONE && initialStatus != UriStatus.UNKNOWN) {
                    Box {
                        OutlinedTextField(
                            value = availableFolders.find { it.id == folderIdState }?.name ?: "Select Folder",
                            onValueChange = { /* Read-only for selection */ },
                            label = { Text("Folder") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true, // Make text field read-only
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Folder") },
                            colors = OutlinedTextFieldDefaults.colors(
                                // Customize colors to look clickable if needed
                            )
                        )
                        // Add clickability overlay or use Modifier.clickable on the TextField itself
                        Box(modifier = Modifier.matchParentSize().clickable { showFolderDropdown = true }) // Overlay for click
                        DropdownMenu(
                            expanded = showFolderDropdown,
                            onDismissRequest = { showFolderDropdown = false }
                        ) {
                            // Option for 'None' or 'Root' folder
                            DropdownMenuItem(
                                text = { Text("None / Root") },
                                onClick = {
                                    folderIdState = null
                                    showFolderDropdown = false
                                }
                            )
                            Divider()
                            availableFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) }, // TODO: Handle nested folder display
                                    onClick = {
                                        folderIdState = folder.id
                                        showFolderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }


                // Preference Options (Only if status is NOT BLOCKED)
                if (initialStatus != UriStatus.BLOCKED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isPreferenceEnabledState,
                            onCheckedChange = { isPreferenceEnabledState = it }
                        )
                        Text("Enable Preference")
                    }
                    Spacer(Modifier.height(8.dp))

                    // Preferred Browser Input/Selector
                    // Option 1: Simple Text Field
                    OutlinedTextField(
                        value = preferredBrowserText,
                        onValueChange = { preferredBrowserText = it },
                        label = { Text("Preferred Browser (Package Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = isPreferenceEnabledState // Only enabled if preference is enabled
                    )
                    // Option 2: Dropdown from available browsers (More complex but better UX)
                    // Box { ... OutlinedTextField with click overlay ... DropdownMenu (items from availableBrowsers) ... }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        hostText,
                        initialStatus, // Use status from screen context
                        folderIdState,
                        preferredBrowserText.takeIf { it.isNotBlank() }, // Save null if empty
                        isPreferenceEnabledState
                    )
                }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// TODO: Add GetAllFoldersByTypeUseCase and update RulesViewModel to use it
// The current RulesViewModel fetches only root folders. For the folder filter
// and the dialog folder picker, we need ALL folders of the relevant type.
// Need to add this Use Case and inject it into RulesViewModel.
// Then update the foldersFlow in RulesViewModel to use it.
// The FolderRepositoryImpl already has getAllFoldersByType.

/*
// domain/usecase/folders/GetAllFoldersByTypeUseCase.kt
interface GetAllFoldersByTypeUseCase {
    operator fun invoke(type: FolderType): Flow<List<Folder>>
}
class GetAllFoldersByTypeUseCaseImpl @Inject constructor(
    private val repository: FolderRepository
): GetAllFoldersByTypeUseCase {
    override fun invoke(type: FolderType): Flow<List<Folder>> {
        Timber.d("Getting all folders of type: $type")
        return repository.getAllFoldersByType(type)
    }
}

// presentation/rules/RulesViewModel.kt
// ... Inject GetAllFoldersByTypeUseCase ...
// Update foldersFlow:
private val foldersFlow: Flow<List<Folder>> = _currentType
    .flatMapLatest { type ->
        val folderType = if (type == UriStatus.BOOKMARKED) FolderType.BOOKMARK else FolderType.BLOCK
        // Use the new Use Case
        getAllFoldersByTypeUseCase(folderType)
            .catch { e ->
                Timber.e(e, "Error fetching all folders")
                // Update UI state with error message
                addMessage("Error loading folders for filter/dialog.", MessageType.ERROR)
                emit(emptyList())
            }
    }
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

// di/UseCaseModule.kt
// Add binding for GetAllFoldersByTypeUseCase
@Provides
@ViewModelScoped
fun provideGetAllFoldersByTypeUseCase(impl: GetAllFoldersByTypeUseCaseImpl): GetAllFoldersByTypeUseCase = impl

*/
