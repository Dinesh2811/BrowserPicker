package browserpicker.presentation.features.browserpicker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.UriSource
import browserpicker.presentation.features.browserpicker.PersistentError.Companion.uiErrorState
import browserpicker.presentation.util.BrowserDefault
import dagger.Binds
import dagger.Module
import dagger.hilt.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.*
import kotlin.collections.map

sealed interface UiError: AppError

sealed interface UiState<out T, out E: UiError> {
    data object Loading: UiState<Nothing, Nothing>
    data object Idle: UiState<Nothing, Nothing>
    data class Success<T>(val data: T): UiState<T, Nothing>
    data class Error<E: UiError>(val error: E): UiState<Nothing, E>
    data object Blocked: UiState<Nothing, Nothing>
}

sealed interface PersistentError: UiError {
    sealed interface InstalledBrowserApps: PersistentError {
        data class Empty(override val message: String = "No installed browser apps found"): InstalledBrowserApps
        data class LoadFailed(override val message: String = "Failed to load installed browser apps", override val cause: Throwable? = null): InstalledBrowserApps
        data class UnknownError(override val message: String = "An unknown error occurred while loading installed browser apps", override val cause: Throwable): InstalledBrowserApps
    }


    data class HostRuleAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError
    data class UnknownHostRuleError(override val message: String, override val cause: Throwable? = null): PersistentError
    data class HistoryUpdateFailed(override val message: String, override val cause: Throwable? = null): PersistentError

    companion object {
        fun uiErrorState(uiState: PersistentError): UiState.Error<UiError> {
            return UiState.Error(uiState)
        }
    }

//    data class InvalidConfiguration(override val message: String = "Invalid browser configuration", override val cause: Throwable? = null): PersistentError
//    data class UnknownError(override val message: String = "An unknown error occurred", override val cause: Throwable? = null): PersistentError
}

enum class TransientError(override val message: String): UiError {
    NULL_OR_EMPTY_URL("URL cannot be empty"),
    NO_BROWSER_SELECTED("Please select a browser first"),
    INVALID_URL_FORMAT("Invalid URL format"),
    LAUNCH_FAILED("Failed to launch browser"),
    SELECTION_REQUIRED("Please select a browser first")
}

sealed interface BrowserPickerUiEffect {
    data object AutoOpenBrowser: BrowserPickerUiEffect
    data object BrowserAppsLoaded: BrowserPickerUiEffect // If loading browser list results in a success state
    data object SettingsSaved: BrowserPickerUiEffect // If saving settings updates this state
    data object UriBookmarked: BrowserPickerUiEffect // If a bookmark action updates the state
    data object UriBlocked: BrowserPickerUiEffect // If a block action updates the state
    data class UriOpenedOnce(val packageName: String): BrowserPickerUiEffect // If opening once needs a specific signal
}

data class BrowserState(
    val allAvailableBrowsers: List<BrowserAppInfo> = emptyList(),
    val selectedBrowserAppInfo: BrowserAppInfo? = null,
    val uiState: UiState<BrowserPickerUiEffect, UiError> = UiState.Idle,
    val searchQuery: String = "",
//    val securityInfoResult: SecurityInfoResult = SecurityInfoResult.Error(SslStatus.FETCH_ERROR, "Not yet fetched", null),
    val uri: Uri? = null,
    val uriProcessingResult: UriProcessingResult? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
//    private val getInstalledBrowserAppsUseCase: GetInstalledBrowserAppsUseCase,
//    private val setDefaultBrowserUseCase: SetDefaultBrowserUseCase,
//    private val launchBrowserUseCase: LaunchBrowserUseCase,
//    private val getUriSecurityInfoUseCase: GetUriSecurityInfoUseCase,
//    private val appRepository: AppRepository,
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val getInstalledBrowserAppsUseCase: GetInstalledBrowserAppsUseCase,
    private val updateUriUseCase: UpdateUriUseCase,
): ViewModel() {
    private val _browserState: MutableStateFlow<BrowserState> = MutableStateFlow(BrowserState(uiState = UiState.Loading))
    val browserState: StateFlow<BrowserState> = _browserState.asStateFlow()

    fun updateBrowserState(newValue: BrowserState) {
        _browserState.update { newValue }
    }

    fun updateBrowserState(update: (BrowserState) -> BrowserState) {
        _browserState.update(update)
    }

    fun updateUri(uri: Uri, source: UriSource = UriSource.INTENT, isUriUpdated: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentState = _browserState.value
//            clearUriSecurityInfo()
            updateUriUseCase(currentState, uri, source)
                .collectLatest { newBrowserState: BrowserState ->
                    _browserState.value = newBrowserState
                    isUriUpdated(newBrowserState.uiState !is UiState.Error<*>)
                    processUri(uri.toString(), source)
                }
        }
    }

//    private fun processUri(uriString: String, source: UriSource) {
//        viewModelScope.launch {
//            val request = UriProcessingRequest(
//                uriString = uriString,
//                uriSource = source,
//                interceptionTime = clock.now()
//            )
//            try {
//                appRepository.getUriProcessingInfo(request)
//                    .flowOn(ioDispatcher) // Ensure DB access is on IO
//                    .catch { e ->
//                        logError("Error getting URI processing info for $uriString from $source \n ${e.message}")
//                        updateErrorUiState(PersistentError.UnknownError(e as? Exception))
//                    }
//                    .collectLatest { result -> // Use collectLatest if multiple updates are possible quickly
//                        _browserState.update { it.copy(uriProcessingResult = result) }
//                        processUriResult(result)
//                    }
//            } catch (e: Exception) {
//                logError("Failed to process URI $uriString from $source \n ${e.message}")
//                updateErrorUiState(PersistentError.UnknownError(e))
//            }
//        }
//    }

    private fun processUri(uriString: String, source: UriSource) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                // TODO: Implement the code here
            }
        }
    }

    fun loadBrowserApps() {
        if (_browserState.value.uiState == UiState.Loading) return
        viewModelScope.launch {
            getInstalledBrowserAppsUseCase()
                .collectLatest { newBrowserState: BrowserState ->
                    _browserState.value = newBrowserState
                }
        }
    }
}

class GetInstalledBrowserAppsUseCase @Inject constructor(
    private val browserPickerRepository: BrowserPickerRepository,
) {
    operator fun invoke(): Flow<BrowserState> {
        val getBrowserAppsResult: Flow<DomainResult<List<BrowserAppInfo>, AppError>> = browserPickerRepository.getInstalledBrowserApps()
        return getBrowserAppsResult
            .map { result: DomainResult<List<BrowserAppInfo>, AppError> ->
                when (result) {
                    is DomainResult.Success -> {
                        val apps: List<BrowserAppInfo> = result.data
                        createBrowserState(
                            allAvailableBrowsers = apps,
                            uiState = if (apps.isEmpty()) {
                                uiErrorState(PersistentError.InstalledBrowserApps.Empty())
                            } else {
                                UiState.Idle
                            }
                        )
                    }

                    is DomainResult.Failure -> {
                        createBrowserState(uiState = uiErrorState(PersistentError.InstalledBrowserApps.LoadFailed(cause = result.error.cause)))
                    }
                }
            }
            .onStart {
                emit(createBrowserState(uiState = UiState.Loading))
            }
            .catch { e ->
                emit(createBrowserState(uiState = uiErrorState(PersistentError.InstalledBrowserApps.UnknownError(cause = e))))
            }
    }

    private fun createBrowserState(
        allAvailableBrowsers: List<BrowserAppInfo> = emptyList(),
        selectedBrowserAppInfo: BrowserAppInfo? = null,
        uiState: UiState<BrowserPickerUiEffect, UiError> = UiState.Idle,
    ): BrowserState {
        return BrowserState(
            allAvailableBrowsers = allAvailableBrowsers,
            selectedBrowserAppInfo = selectedBrowserAppInfo,
            uiState = uiState
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowserPickerModule {
    @Binds
    @Singleton
    abstract fun bindBrowserPickerRepository(browserRepositoryImpl: BrowserPickerRepositoryImpl): BrowserPickerRepository
}


interface BrowserPickerRepository {
    fun getInstalledBrowserApps(): Flow<DomainResult<List<BrowserAppInfo>, AppError>>
}

class BrowserPickerRepositoryImpl @Inject constructor(
    private val browserPickerDataSource: BrowserPickerDataSource,
    private val browserPickerAppMapper: BrowserPickerAppMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
): BrowserPickerRepository {
    override fun getInstalledBrowserApps(): Flow<DomainResult<List<BrowserAppInfo>, AppError>> = flow {
        try {
            val browserApps: List<BrowserAppInfo> = coroutineScope {
                browserPickerDataSource.browserResolveInfoList
                    .asSequence()
                    .filter { isValidBrowserApp(it) }
                    .map { resolveInfo ->
                        async(defaultDispatcher) { createBrowserAppInfo(resolveInfo) }
                    }
                    .toList()
                    .awaitAll()
                    .filterNotNull()
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            }

            emit(DomainResult.Success(browserApps))
        } catch (e: Exception) {
            val message = "An unexpected error occurred while fetching installed browser apps"
            Timber.e(e, "$message: ${e.message}")
            emit(DomainResult.Failure(AppError.UnknownError(message = message, cause = e)))
        }
    }.flowOn(defaultDispatcher)

    private suspend fun createBrowserAppInfo(resolveInfo: ResolveInfo): BrowserAppInfo? {
        return try {
            val activityInfo = resolveInfo.activityInfo ?: return null
            val packageName = browserPickerDataSource.getPackageName(activityInfo) ?: return null
            val appName = browserPickerDataSource.getAppName(resolveInfo) ?: return null
            val appIcon = browserPickerDataSource.getAppIcon(resolveInfo)
            val isDefaultBrowser = browserPickerDataSource.isDefaultBrowser(packageName, 0)

            browserPickerAppMapper.mapToDomain(
                BrowserAppInfoEntity(
                    appName = appName,
                    packageName = packageName,
                    isDefaultBrowser = isDefaultBrowser
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating BrowserAppInfo for ${resolveInfo.activityInfo?.packageName}: ${e.message}")
            null
        }
    }

    private fun isValidBrowserApp(resolveInfo: ResolveInfo): Boolean {
        return try {
            browserPickerDataSource.isBrowserApp(resolveInfo.activityInfo) &&
                    resolveInfo.activityInfo.packageName != BrowserDefault.CURRENT_PACKAGE
        } catch (e: Exception) {
            Timber.e(e, "Error validating BrowserAppInfo for ${resolveInfo.activityInfo?.packageName}: ${e.message}")
            false
        }
    }

    private fun createBrowserAppInfoEntity(resolveInfo: ResolveInfo): BrowserAppInfoEntity? {
        return try {
            val activityInfo = resolveInfo.activityInfo ?: return null
            val packageName = browserPickerDataSource.getPackageName(activityInfo) ?: return null
            val appName = browserPickerDataSource.getAppName(resolveInfo) ?: return null
            val appIcon = browserPickerDataSource.getAppIcon(resolveInfo)
            val isDefaultBrowser = browserPickerDataSource.isDefaultBrowser(packageName, 0)

            BrowserAppInfoEntity(
                appName = appName,
                packageName = packageName,
                isDefaultBrowser = isDefaultBrowser
            )
        } catch (e: Exception) {
            null
        }
    }
}

class BrowserPickerDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "log_BrowserDataSource"
    }

    val packageManager: PackageManager by lazy { context.packageManager }
    val browserResolveInfoList: List<ResolveInfo> by lazy {
//        browserResolveInfoList.forEach {
//            val componentName = ComponentName("packageName", it.activityInfo.name)
//            val filter = IntentFilter(Intent.ACTION_VIEW).apply {
//                addCategory(Intent.CATEGORY_BROWSABLE)
//                addDataScheme("http")
//                addDataScheme("https")
//            }
//            context.packageManager.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_SCHEME, arrayOf(componentName), componentName)
////            Toast.makeText(context, "Default browser set to ${appName}", Toast.LENGTH_SHORT).show()
//        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.MATCH_ALL)
            }
        } catch (e: Exception) {
            LogLevel.Error.log("Error querying browser intents: ${e.message}", TAG)
            emptyList()
        }
    }

    fun createBrowserIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, "https://".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    fun getAppName(resolveInfo: ResolveInfo): String? {
        return try {
            resolveInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            LogLevel.Error.log("Error getting app name: ${e.message}", TAG)
            null
        }
    }

    fun getPackageName(activityInfo: ActivityInfo): String? {
        return try {
            activityInfo.packageName
        } catch (e: Exception) {
            LogLevel.Error.log("Error getting app name: ${e.message}", TAG)
            null
        }
    }

    fun getAppIcon(resolveInfo: ResolveInfo): Drawable? {
        return try {
            resolveInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            LogLevel.Error.log("Unexpected exception: ${e.message}", TAG)
            null
        }
    }

    fun isDefaultBrowser(packageName: String?, flag: Int): Boolean {
        if (packageName.isNullOrEmpty()) return false
//        val resolveInfo = packageManager.resolveActivity(createBrowserIntent(), PackageManager.MATCH_DEFAULT_ONLY)
        val resolveInfo = packageManager.resolveActivity(createBrowserIntent(), flag)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    fun isBrowserApp(activityInfo: ActivityInfo): Boolean {
//        val intentFilters = activityInfo.metaData
//        return intentFilters != null && intentFilters.containsKey("android.intent.category.BROWSABLE")

//        activityInfo.packageName?.isNotEmpty() == true &&
//                getPackageInfo(activityInfo.packageName, PackageManager.GET_PERMISSIONS)
//                    ?.requestedPermissions
//                    ?.contains(android.Manifest.permission.INTERNET) == true
        return try {
            createBrowserIntent().apply { `package` = activityInfo.packageName }.resolveActivity(packageManager) != null &&
                    activityInfo.exported == true &&
                    activityInfo.permission == null || activityInfo.permission == "android.permission.INTERNET"
        } catch (e: Exception) {
            LogLevel.Error.log("Failed to check browser app: ${e.message}", TAG)
            false
        }
    }

    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? = try {
        when {
            packageName.isEmpty() -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
        }
    } catch (e: PackageManager.NameNotFoundException) {
        LogLevel.Error.log("Name not found: ${e.message}", TAG)
        null
    } catch (e: SecurityException) {
        LogLevel.Error.log("Security exception: ${e.message}", TAG)
        null
    } catch (e: Exception) {
        LogLevel.Error.log("Unexpected exception: ${e.message}", TAG)
        null
    }

    suspend fun setDefaultBrowser(packageName: String) = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val componentName = ComponentName(packageName, "$packageName.MainActivity")
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        // Additional logic to set the default browser (e.g., using RoleManager on Android 10+)
    }

//    fun setDefaultBrowser(context: Context, url: String, selectedApp: BrowserAppInfoEntity?) {
//        selectedApp ?: return
//        // Clear the previous default (if any)
//        val clearIntent = Intent(Intent.ACTION_VIEW, url.toUri())
//        val clearResolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            context.packageManager.queryIntentActivities(clearIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
//        } else {
//            context.packageManager.queryIntentActivities(clearIntent, PackageManager.MATCH_DEFAULT_ONLY)
//        }
//        for (resolveInfo in clearResolveList) {
//            val packageName = resolveInfo.activityInfo.packageName
//            if (packageName != selectedApp.packageName) {
//                val componentName = ComponentName(packageName, resolveInfo.activityInfo.name)
//                context.packageManager.clearPackagePreferredActivities(componentName.packageName)
//            }
//        }
//    }
}

data class BrowserAppInfoEntity(
    val appName: String?,
    val packageName: String?,
    val isDefaultBrowser: Boolean = false,
)

class BrowserPickerAppMapper @Inject constructor() {
    private val TAG = "log_BrowserPickerAppMapper"

    suspend fun mapToDomain(entity: BrowserAppInfoEntity): BrowserAppInfo? = coroutineScope {
        try {
            entity.run {
                BrowserAppInfo(
                    packageName = packageName ?: return@coroutineScope null,
                    appName = appName ?: return@coroutineScope null,
                )
            }
        } catch (e: Exception) {
            LogLevel.Error.log("Error mapping entity: ${e.message}", TAG)
            null
        }
    }

    suspend fun mapToDomain(entities: List<BrowserAppInfoEntity>): List<BrowserAppInfo> = coroutineScope {
        if (entities.isEmpty()) {
            return@coroutineScope emptyList()
        }

        try {
            val mappedList = try {
                if (entities.size > 10) {
                    // Parallel processing for large lists
                    entities.map { entity ->
                        async(Dispatchers.Default) { mapToDomain(entity) }
                    }.awaitAll().filterNotNull()
                } else {
                    // Sequential processing for small lists
                    entities.mapNotNull { entity -> mapToDomain(entity) }
                }
            } catch (e: Exception) {
                LogLevel.Error.log("Error during parallel mapping: ${e.message}", TAG)
                emptyList<BrowserAppInfo>()
            }

            mappedList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
        } catch (e: Exception) {
            LogLevel.Error.log("Error mapping entities: ${e.message}", TAG)
            emptyList()
        }
    }

    private fun mapToEntity(domain: BrowserAppInfo): BrowserAppInfoEntity {
        return BrowserAppInfoEntity(
            appName = domain.appName,
            packageName = domain.packageName,
        )
    }

    private fun mapToEntity(domains: List<BrowserAppInfo>): List<BrowserAppInfoEntity> {
        return domains.asSequence()
            .map { mapToEntity(it) }
            .toList()
    }
}
