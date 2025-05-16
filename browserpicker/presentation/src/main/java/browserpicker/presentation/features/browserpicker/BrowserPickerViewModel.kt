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
import browserpicker.core.results.PersistentError
import browserpicker.core.results.TransientError
import browserpicker.core.results.UiError
import browserpicker.core.results.UiState
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import browserpicker.core.utils.logDebug
import browserpicker.core.utils.logError
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.UriSource
import browserpicker.presentation.toUiState
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import timber.log.Timber
import javax.inject.*
import kotlin.collections.map
import kotlin.time.Duration.Companion.milliseconds

data class BrowserState(
    val allAvailableBrowsers: List<BrowserAppInfo> = emptyList(),
    val selectedBrowserAppInfo: BrowserAppInfo? = null,
    val uiState: UiState<Unit, UiError> = UiState.Idle,
    val searchQuery: String = "",
//    val securityInfoResult: SecurityInfoResult = SecurityInfoResult.Error(SslStatus.FETCH_ERROR, "Not yet fetched", null),
    val uri: Uri? = null,
    val uriSource: UriSource = UriSource.INTENT,
    val uriProcessingResult: UriProcessingResult? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    private val getInstalledBrowserAppsUseCase: GetInstalledBrowserAppsUseCase,
//    private val setDefaultBrowserUseCase: SetDefaultBrowserUseCase,
//    private val launchBrowserUseCase: LaunchBrowserUseCase,
//    private val getUriSecurityInfoUseCase: GetUriSecurityInfoUseCase,
//    private val appRepository: AppRepository,
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val loadBrowserAppsUseCase: LoadBrowserAppsUseCase,
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
//            clearUriSecurityInfo()
            val uriString = uri.toString()
            if (BrowserDefault.isValidUrl(uriString)) {
                _browserState.update {
                    it.copy(
                        uri = uri,
                        uriSource = source,
                        uriProcessingResult = null
                    )
                }
                isUriUpdated(true)
                processUri(uriString, source)
            } else {
                isUriUpdated(false)
                _browserState.update { it.copy(uiState = UiState.Error(TransientError.INVALID_URL_FORMAT)) }
            }
        }
    }

    private fun processUri(uriString: String, source: UriSource) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                // TODO: Implement the code here
            }
        }
    }

    private fun updateErrorUiState(uiState: UiError) {
        _browserState.update { it.copy(uiState = UiState.Error(uiState)) }
    }

    fun loadBrowserApps1() {
        if (_browserState.value.uiState !is UiState.Loading) {
            _browserState.update { it.copy(uiState = UiState.Loading) }
        }
        viewModelScope.launch {
            try {
                getInstalledBrowserAppsUseCase()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        updateErrorUiState(PersistentError.LoadFailed(cause = e))
                    }
                    .collectLatest { apps ->
                        _browserState.update {
                            it.copy(
                                allAvailableBrowsers = apps,
                                uiState = if (apps.isEmpty()) { UiState.Error(PersistentError.NoBrowserAppsFound()) } else { UiState.Idle },
                                selectedBrowserAppInfo = null
                            )
                        }
                        logDebug("Browser apps loaded. Count: ${apps.size}")
                    }
            } catch (e: Exception) {
                updateErrorUiState(PersistentError.LoadFailed(cause = e))
            }
        }
    }

    fun loadBrowserApps() {
        viewModelScope.launch {
            loadBrowserAppsUseCase()
                .collectLatest { newBrowserState: BrowserState ->
                    _browserState.value = newBrowserState
                }
        }
    }

//    fun loadBrowserApps() {
//        if (_browserState.value.uiState is UiState.Loading) {
//            return
//        }
//
//        viewModelScope.launch {
//            _browserState.update { it.copy(uiState = UiState.Loading) }
//
//            loadBrowserAppsUseCase()
//                .collectLatest { result: DomainResult<List<BrowserAppInfo>, PersistentError> ->
//                    _browserState.update { currentState: BrowserState ->
//                        result.fold(
//                            onSuccess = { apps: List<BrowserAppInfo> ->
//                                Timber.d("Browser apps loaded. Count: ${apps.size}")
//                                currentState.copy(
//                                    allAvailableBrowsers = apps,
//                                    uiState = if (apps.isEmpty()) { UiState.Error(PersistentError.NoBrowserAppsFound()) } else { UiState.Idle },
//                                    selectedBrowserAppInfo = null
//                                )
//                            },
//                            onFailure = { error: PersistentError ->
//                                currentState.copy(
//                                    allAvailableBrowsers = emptyList(),
//                                    uiState = UiState.Error(error),
//                                    selectedBrowserAppInfo = null
//                                )
//                            }
//                        )
//                    }
//                }
//        }
//    }
}

class LoadBrowserAppsUseCase @Inject constructor(
    private val browserPickerRepository: BrowserPickerRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
//    operator fun invoke(): Flow<DomainResult<List<BrowserAppInfo>, PersistentError>> {
//        val browserAppInfos: Flow<List<BrowserAppInfo>> = browserPickerRepository.getBrowserApps()
//        return browserAppInfos
//            .flowOn(ioDispatcher)
//            .map<List<BrowserAppInfo>, DomainResult<List<BrowserAppInfo>, PersistentError>> { apps ->
//                DomainResult.Success(apps)
//            }
//            .catch { e ->
//                emit(DomainResult.Failure(PersistentError.LoadFailed(cause = e)))
//            }
//    }
/*
    operator fun invoke(): Flow<BrowserState> {
        return flow {
            val browserAppInfos: Flow<List<BrowserAppInfo>> = browserPickerRepository.getBrowserApps()
            browserAppInfos
                .flowOn(ioDispatcher)
                .collect { apps: List<BrowserAppInfo> ->
                    if (apps.isEmpty()) {
                        emit(BrowserState(
                            allAvailableBrowsers = emptyList(),
                            uiState = UiState.Error(PersistentError.NoBrowserAppsFound()),
                            selectedBrowserAppInfo = null
                        ))
                    } else {
                        emit(BrowserState(
                            allAvailableBrowsers = apps,
                            uiState = UiState.Idle,
                            selectedBrowserAppInfo = null
                        ))
                    }
                }
        }
            .onStart {
                emit(BrowserState(
                    allAvailableBrowsers = emptyList(),
                    uiState = UiState.Loading,
                    selectedBrowserAppInfo = null
                ))
            }
            .catch { e ->
                emit(BrowserState(
                    allAvailableBrowsers = emptyList(),
                    uiState = UiState.Error(PersistentError.LoadFailed(cause = e)),
                    selectedBrowserAppInfo = null
                ))
            }
    }

 */

    operator fun invoke(): Flow<BrowserState> {
        return browserPickerRepository.getBrowserApps()
            .flowOn(ioDispatcher)
            .map { apps: List<BrowserAppInfo> ->
                if (apps.isEmpty()) {
                    BrowserState(
                        allAvailableBrowsers = emptyList(),
                        uiState = UiState.Error(PersistentError.NoBrowserAppsFound()),
                        selectedBrowserAppInfo = null
                    )
                } else {
                    BrowserState(
                        allAvailableBrowsers = apps,
                        uiState = UiState.Idle,
                        selectedBrowserAppInfo = null
                    )
                }
            }
            .onStart {
                emit(BrowserState(
                    allAvailableBrowsers = emptyList(),
                    uiState = UiState.Loading,
                    selectedBrowserAppInfo = null
                ))
            }
            .catch { e ->
                emit(BrowserState(
                    allAvailableBrowsers = emptyList(),
                    uiState = UiState.Error(PersistentError.LoadFailed(cause = e)),
                    selectedBrowserAppInfo = null
                ))
            }
    }
}

class GetInstalledBrowserAppsUseCase @Inject constructor(
    private val browserPickerRepository: BrowserPickerRepository,
) {
    operator fun invoke(): Flow<List<BrowserAppInfo>> {
        return browserPickerRepository.getBrowserApps()
            .map { browserList ->
                browserList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            }
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
    fun getBrowserApps(): Flow<List<BrowserAppInfo>>
}

class BrowserPickerRepositoryImpl @Inject constructor(
    private val browserPickerDataSource: BrowserPickerDataSource,
    private val browserPickerAppMapper: BrowserPickerAppMapper,
): BrowserPickerRepository {
    private val TAG = "log_BrowserRepositoryImpl"

    override fun getBrowserApps(): Flow<List<BrowserAppInfo>> = flow {
        try {
            val browserApps: List<BrowserAppInfo> = coroutineScope {
                browserPickerDataSource.browserResolveInfoList
                    .asSequence()
                    .filter { isValidBrowserApp(it) }
                    .map { resolveInfo ->
                        async(Dispatchers.Default) { createBrowserAppInfo(resolveInfo) }
                    }
                    .toList()
                    .awaitAll()
                    .filterNotNull()
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            }

            emit(browserApps)
        } catch (e: Exception) {
            LogLevel.Error.log("Error fetching browser apps: ${e.message}", TAG)
            emit(emptyList<BrowserAppInfo>())
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun createBrowserAppInfo(resolveInfo: ResolveInfo): BrowserAppInfo? {
        return try {
            val activityInfo = resolveInfo.activityInfo?: return null
            val packageName = browserPickerDataSource.getPackageName(activityInfo)?: return null
            val appName = browserPickerDataSource.getAppName(resolveInfo)?: return null
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
            null
        }
    }

    private fun isValidBrowserApp(resolveInfo: ResolveInfo): Boolean {
        return try {
            browserPickerDataSource.isBrowserApp(resolveInfo.activityInfo) &&
                    resolveInfo.activityInfo.packageName != BrowserDefault.CURRENT_PACKAGE
        } catch (e: Exception) {
            false
        }
    }

    private fun createBrowserAppInfoEntity(resolveInfo: ResolveInfo): BrowserAppInfoEntity? {
        return try {
            val activityInfo = resolveInfo.activityInfo?: return null
            val packageName = browserPickerDataSource.getPackageName(activityInfo)?: return null
            val appName = browserPickerDataSource.getAppName(resolveInfo)?: return null
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
                    packageName = packageName?: return@coroutineScope null,
                    appName = appName?: return@coroutineScope null,
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
