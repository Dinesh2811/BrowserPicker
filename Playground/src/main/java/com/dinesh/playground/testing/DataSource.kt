package com.dinesh.playground.testing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Interface for accessing locally persisted data (e.g., Room Database).
 * Methods operate on data layer entities (e.g., UriEntryEntity, FolderEntity).
 */
interface LocalDataSource {

    // --- UriEntry Operations ---

    fun observeUriEntries(statusFilter: UriStatus?, searchQuery: String?): Flow<List<UriEntryEntity>>
    fun observeUriEntriesWithFolders(statusFilter: UriStatus?, searchQuery: String?): Flow<List<UriEntryWithFolderEntity>>
    suspend fun findUriEntryByUriString(uriString: String): UriEntryEntity?
    suspend fun getUriEntryById(id: String): UriEntryEntity?
    suspend fun insertUriEntry(uriEntry: UriEntryEntity): Long
    suspend fun updateUriEntry(uriEntry: UriEntryEntity) // Full update based on primary key
    suspend fun updateUriStatusAndFolder(uriString: String, newStatus: UriStatus, newFolderId: String?, statusUpdatedAt: Instant)
    suspend fun updateUriFolder(uriString: String, newFolderId: String, statusUpdatedAt: Instant)
    suspend fun updateLastAccessedTime(uriString: String, lastAccessedAt: Instant)
    suspend fun deleteUriEntryByUriString(uriString: String)
    suspend fun deleteUriEntriesByFolderId(folderId: String): Int
    suspend fun moveUriEntriesFolder(oldFolderId: String, newFolderId: String?, statusUpdatedAt: Instant): Int

    // --- Folder Operations ---

    fun observeFolders(typeFilter: FolderType?, parentIdFilter: String?): Flow<List<FolderEntity>>
    suspend fun getFolderById(id: String): FolderEntity?
    suspend fun findFolderByNameAndParent(name: String, type: FolderType, parentId: String?): FolderEntity?
    suspend fun insertFolder(folder: FolderEntity): Long // Return value might be row ID from DB
    suspend fun updateFolder(folder: FolderEntity)
    suspend fun deleteFolderById(folderId: String)
    suspend fun findFoldersByParentId(parentId: String): List<FolderEntity> // Needed for cascading delete logic potentially

    // --- UriPreference Operations ---

    fun observePreferences(): Flow<List<UriPreferenceEntity>>
    suspend fun findPreferenceByUri(uriString: String): UriPreferenceEntity?
    suspend fun insertPreference(preference: UriPreferenceEntity)
    suspend fun deletePreferenceByUriString(uriString: String)
    suspend fun deletePreferencesByPackageName(packageName: String): Int

}

/**
 * Interface for accessing data provided by the Android system.
 */
interface SystemDataSource {

    /**
     * Retrieves applications capable of handling VIEW intents for http/https schemes.
     */
    suspend fun getInstalledBrowsers(): List<BrowserApp> // Returns Domain model directly is acceptable here

    /**
     * Gets the package name of the current default browser set in the system.
     */
    suspend fun getDefaultBrowserPackageName(): String?

    /**
     * Observes the clipboard content for potential URIs.
     * Emits the URI string if a valid web URI is detected on the clipboard.
     * Note: Requires careful permission handling and background execution strategy.
     * Consider privacy implications (Clipboard access warnings on Android 10+).
     */
    fun observeClipboardUris(): Flow<String> // Emits URI strings found

    /**
     * Gets the current primary clip content as text.
     * Used for manual checking or on-demand clipboard access.
     * Returns null if clipboard is empty or content is not text.
     */
    suspend fun getCurrentClipboardText(): String?
}


/**
 * Implementation of LocalDataSource using Room database DAOs.
 */
class LocalDataSourceImpl @Inject constructor(
    private val uriEntryDao: UriEntryDao,
    private val folderDao: FolderDao,
    private val uriPreferenceDao: UriPreferenceDao,
    // Inject dispatcher for DB operations - typically Dispatchers.IO
    private val ioDispatcher: CoroutineDispatcher
) : LocalDataSource {

    // --- UriEntry Operations ---

    override fun observeUriEntries(statusFilter: UriStatus?, searchQuery: String?): Flow<List<UriEntryEntity>> =
        uriEntryDao.observeAllFiltered(statusFilter, searchQuery)

    override fun observeUriEntriesWithFolders(statusFilter: UriStatus?, searchQuery: String?): Flow<List<UriEntryWithFolderEntity>> =
        uriEntryDao.observeAllWithFoldersFiltered(statusFilter, searchQuery)


    override suspend fun findUriEntryByUriString(uriString: String): UriEntryEntity? = withContext(ioDispatcher) {
        uriEntryDao.findByUriString(uriString)
    }

    override suspend fun getUriEntryById(id: String): UriEntryEntity? = withContext(ioDispatcher) {
        uriEntryDao.getById(id)
    }

    override suspend fun insertUriEntry(uriEntry: UriEntryEntity) = withContext(ioDispatcher) {
        uriEntryDao.insert(uriEntry)
    }

    override suspend fun updateUriEntry(uriEntry: UriEntryEntity) = withContext(ioDispatcher) {
        uriEntryDao.update(uriEntry)
    }

    override suspend fun updateUriStatusAndFolder(uriString: String, newStatus: UriStatus, newFolderId: String?, statusUpdatedAt: Instant) = withContext(ioDispatcher) {
        uriEntryDao.updateStatusAndFolder(uriString, newStatus, newFolderId, statusUpdatedAt)
    }

    override suspend fun updateUriFolder(uriString: String, newFolderId: String, statusUpdatedAt: Instant) = withContext(ioDispatcher) {
        uriEntryDao.updateFolder(uriString, newFolderId, statusUpdatedAt)
    }

    override suspend fun updateLastAccessedTime(uriString: String, lastAccessedAt: Instant) = withContext(ioDispatcher) {
        uriEntryDao.updateLastAccessedTime(uriString, lastAccessedAt)
    }

    override suspend fun deleteUriEntryByUriString(uriString: String) = withContext(ioDispatcher) {
        uriEntryDao.deleteByUriString(uriString)
    }

    override suspend fun deleteUriEntriesByFolderId(folderId: String): Int = withContext(ioDispatcher) {
        uriEntryDao.deleteByFolderId(folderId)
    }

    override suspend fun moveUriEntriesFolder(oldFolderId: String, newFolderId: String?, statusUpdatedAt: Instant): Int = withContext(ioDispatcher) {
        uriEntryDao.updateFolderIdForEntries(oldFolderId, newFolderId, statusUpdatedAt)
    }

    // --- Folder Operations ---

    override fun observeFolders(typeFilter: FolderType?, parentIdFilter: String?): Flow<List<FolderEntity>> =
        folderDao.observeAllFiltered(typeFilter, parentIdFilter)


    override suspend fun getFolderById(id: String): FolderEntity? = withContext(ioDispatcher) {
        folderDao.getById(id)
    }

    override suspend fun findFolderByNameAndParent(name: String, type: FolderType, parentId: String?): FolderEntity? = withContext(ioDispatcher) {
        folderDao.findByNameAndParent(name, type, parentId)
    }

    override suspend fun insertFolder(folder: FolderEntity): Long = withContext(ioDispatcher) {
        folderDao.insert(folder)
    }

    override suspend fun updateFolder(folder: FolderEntity) = withContext(ioDispatcher) {
        folderDao.update(folder)
    }

    override suspend fun deleteFolderById(folderId: String) = withContext(ioDispatcher) {
        folderDao.deleteById(folderId)
    }

    override suspend fun findFoldersByParentId(parentId: String): List<FolderEntity> = withContext(ioDispatcher) {
        folderDao.findByParentId(parentId) // Assuming this DAO method exists
    }


    // --- UriPreference Operations ---

    override fun observePreferences(): Flow<List<UriPreferenceEntity>> = uriPreferenceDao.observeAll()

    override suspend fun findPreferenceByUri(uriString: String): UriPreferenceEntity? = withContext(ioDispatcher) {
        uriPreferenceDao.findByUriString(uriString)
    }

    override suspend fun insertPreference(preference: UriPreferenceEntity) = withContext(ioDispatcher) {
        uriPreferenceDao.insert(preference)
    }

    override suspend fun deletePreferenceByUriString(uriString: String) = withContext(ioDispatcher) {
        uriPreferenceDao.deleteByUriString(uriString)
    }

    override suspend fun deletePreferencesByPackageName(packageName: String): Int = withContext(ioDispatcher) {
        uriPreferenceDao.deleteByPackageName(packageName)
    }
}


/**
 * Implementation of SystemDataSource using Android framework APIs.
 */
class SystemDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher // For PackageManager potentially blocking calls
) : SystemDataSource {

    private val packageManager: PackageManager = context.packageManager
    private val clipboardManager: ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override suspend fun getInstalledBrowsers(): List<BrowserApp> = withContext(ioDispatcher) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            data = Uri.parse("https://") // Generic https scheme
        }

        val resolveInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        val defaultBrowserPackage = getDefaultBrowserPackageName() // Get default browser once

        resolveInfoList.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            if (activityInfo != null) {
                try {
                    val appInfo = packageManager.getApplicationInfo(activityInfo.packageName, 0)
                    val userFriendlyName = packageManager.getApplicationLabel(appInfo).toString()
                    BrowserApp(
                        packageName = activityInfo.packageName,
                        activityName = activityInfo.name, // Full class name
                        userFriendlyName = userFriendlyName,
                        isDefaultBrowser = activityInfo.packageName == defaultBrowserPackage
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // Should not happen if resolveInfo is valid, but handle defensively
                    null
                }
            } else {
                null
            }
        }.distinctBy { it.packageName } // Ensure unique browsers based on package name
            .sortedBy { it.userFriendlyName } // Sort alphabetically for consistent display
    }

    override suspend fun getDefaultBrowserPackageName(): String? = withContext(ioDispatcher) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
        val resolveInfo: ResolveInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        // The default browser's activity info might have a specific match type or be the only one if only one browser exists
        // If multiple browsers exist and one is default, resolveActivity returns the default.
        // If multiple exist and none is default, it might return null or a system chooser activity.
        // We only want the actual browser package name.
        resolveInfo?.activityInfo?.packageName?.takeIf { it != "android" } // Exclude system chooser
    }

    // Basic implementation. Consider throttling/debouncing in a real app.
    // WARNING: Accessing clipboard in background has restrictions (Android 10+).
    // This flow might only work effectively when the app is in the foreground.
    // A foreground service might be needed for true background monitoring, adding complexity.
    override fun observeClipboardUris(): Flow<String> = callbackFlow<String> {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(context)?.toString()
                if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
                    // Basic validation, more robust URI parsing might be needed
                    trySend(text) // Send the potential URI
                }
            }
        }

        // Send initial value if present
        sendCurrentClipboardUri()

        clipboardManager.addPrimaryClipChangedListener(listener)

        // Remove listener when the flow collector cancels
        awaitClose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }.distinctUntilChanged() // Only emit when the URI actually changes


    // Helper to send current clipboard content if it's a URI
    private suspend fun callbackFlowSendCurrentClipboardUri(scope: kotlinx.coroutines.channels.ProducerScope<String>) {
        val text = getCurrentClipboardText()
        if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
            scope.trySend(text)
        }
    }
    // Helper to send current clipboard content if it's a URI
    private suspend fun kotlinx.coroutines.channels.ProducerScope<String>.sendCurrentClipboardUri() {
        val text = getCurrentClipboardText() // Reuse the suspend function
        if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
            trySend(text).isSuccess // Use trySend for safety in callbackFlow
        }
    }

    override suspend fun getCurrentClipboardText(): String? = withContext(ioDispatcher) {
        if (!clipboardManager.hasPrimaryClip()) {
            return@withContext null
        }
        val clip: ClipData? = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            // Attempt to coerce the first item to text.
            // Use `coerceToText` which handles different MIME types gracefully.
            clip.getItemAt(0).coerceToText(context)?.toString()
        } else {
            null
        }
    }
}
