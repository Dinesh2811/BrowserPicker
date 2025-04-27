package com.dinesh.playground.testing

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing URI history entries (`UriEntry`).
 */
interface UriHistoryRepository {

    /**
     * Observes all URI entries, potentially filtered by status or search query.
     * Emits a new list whenever the underlying data changes.
     *
     * @param statusFilter Optional filter by UriStatus.
     * @param searchQuery Optional text search query (e.g., matching parts of the URI).
     * @return A Flow emitting the list of matching UriEntry objects.
     */
    fun observeUriHistory(
        statusFilter: UriStatus? = null,
        searchQuery: String? = null
    ): Flow<List<UriEntry>>

    /**
     * Observes URI entries combined with their associated folder details.
     * Useful for displaying history lists where folder names are needed.
     *
     * @param statusFilter Optional filter by UriStatus.
     * @param searchQuery Optional text search query.
     * @return A Flow emitting the list of matching UriEntryWithFolder objects.
     */
    fun observeUriHistoryWithFolders(
        statusFilter: UriStatus? = null,
        searchQuery: String? = null
    ): Flow<List<UriEntryWithFolder>>


    /**
     * Finds a specific URI entry by its URI string.
     * Returns null if no entry exists for the given URI string.
     *
     * @param uriString The exact URI string to look for.
     * @return The matching UriEntry, or null if not found.
     */
    suspend fun findByUriString(uriString: String): UriEntry?

    /**
     * Gets a specific URI entry by its ID.
     * Throws exception or returns null if not found (implementation detail).
     *
     * @param id The unique ID of the UriEntry.
     * @return The UriEntry.
     */
    suspend fun getById(id: String): UriEntry? // Or throw exception if preferred

    /**
     * Saves or updates a UriEntry. If an entry with the same URI string exists,
     * it might be updated; otherwise, a new entry is created.
     * Consider timestamp updates internally.
     *
     * @param uriEntry The UriEntry object to save or update.
     */
    suspend fun saveUriEntry(uriEntry: UriEntry)

    /**
     * Updates the status and potentially the folder association of a URI entry.
     * Handles timestamp updates for `statusUpdatedAt`.
     *
     * @param uriString The URI string of the entry to update.
     * @param newStatus The new UriStatus.
     * @param newFolderId The ID of the associated folder (required if status is BOOKMARKED or BLOCKED, null otherwise).
     */
    suspend fun updateUriStatus(uriString: String, newStatus: UriStatus, newFolderId: String?)

    /**
     * Updates the folder association for an existing bookmarked or blocked URI.
     *
     * @param uriString The URI string of the entry to update.
     * @param newFolderId The ID of the new folder.
     */
    suspend fun updateUriFolder(uriString: String, newFolderId: String)

    /**
     * Updates the last accessed timestamp for a URI entry.
     *
     * @param uriString The URI string of the entry to update.
     */
    suspend fun updateLastAccessedTime(uriString: String)


    /**
     * Deletes a URI entry permanently from history.
     *
     * @param uriString The URI string of the entry to delete.
     */
    suspend fun deleteUriEntry(uriString: String)

    /**
     * Deletes all URI entries associated with a specific folder ID.
     * Used when a folder is deleted.
     *
     * @param folderId The ID of the folder whose URIs should be deleted.
     * @return The number of deleted entries.
     */
    suspend fun deleteUriEntriesByFolderId(folderId: String): Int

    /**
     * Changes the folder ID for all URIs currently in a specific folder.
     * Used when merging folders or moving URIs to a default folder upon parent deletion.
     *
     * @param oldFolderId The current folder ID of the URIs.
     * @param newFolderId The target folder ID (can be null to unassign).
     * @return The number of updated entries.
     */
    suspend fun moveUriEntriesToFolder(oldFolderId: String, newFolderId: String?): Int
}

/**
 * Repository interface for managing Folders.
 */
interface FolderRepository {

    /**
     * Observes all folders, potentially filtered by type and parent ID.
     * Emits a new list whenever the underlying data changes.
     *
     * @param typeFilter Optional filter by FolderType.
     * @param parentIdFilter Optional filter for direct children of a specific parent folder (null for root folders).
     * @return A Flow emitting the list of matching Folder objects.
     */
    fun observeFolders(
        typeFilter: FolderType? = null,
        parentIdFilter: String? = null // Use null to get root folders
    ): Flow<List<Folder>>

    /**
     * Gets a specific folder by its ID.
     *
     * @param id The unique ID of the folder.
     * @return The Folder object, or null if not found.
     */
    suspend fun getFolderById(id: String): Folder?

    /**
     * Finds a folder by its name, type, and parent ID. Useful for checking duplicates before creation.
     *
     * @param name The name of the folder.
     * @param type The type of the folder.
     * @param parentId The ID of the parent folder (null for root).
     * @return The matching Folder object, or null if not found.
     */
    suspend fun findFolder(name: String, type: FolderType, parentId: String?): Folder?

    /**
     * Creates a new folder. Implementation should handle ID generation and timestamps.
     *
     * @param folder The Folder object to create (ID might be ignored/generated by implementation).
     * @return The created Folder object with its assigned ID and timestamps.
     */
    suspend fun createFolder(folder: Folder): Folder

    /**
     * Updates an existing folder (e.g., rename, move). Handles `updatedAt` timestamp.
     *
     * @param folder The Folder object with updated information.
     */
    suspend fun updateFolder(folder: Folder)

    /**
     * Deletes a folder by its ID.
     * **Note:** The Use Case layer will be responsible for handling nested folders and
     * the URIs within the deleted folder (e.g., deleting them or moving them).
     * This repository method focuses solely on deleting the folder record itself.
     *
     * @param folderId The ID of the folder to delete.
     */
    suspend fun deleteFolder(folderId: String)

    /**
     * Gets the default folder ID for a given type (Bookmark or Block).
     * The repository implementation might create these default folders if they don't exist.
     *
     * @param type The type of the default folder required.
     * @return The ID of the default folder.
     */
    suspend fun getDefaultFolderId(type: FolderType): String
}

/**
 * Repository interface for managing URI-specific browser preferences.
 */
interface UriPreferenceRepository {

    /**
     * Observes all URI preferences.
     * @return A Flow emitting the list of all UriPreference objects.
     */
    fun observeAllPreferences(): Flow<List<UriPreference>>

    /**
     * Finds the preference associated with a specific URI string.
     *
     * @param uriString The exact URI string to look for.
     * @return The matching UriPreference, or null if no preference is set.
     */
    suspend fun findPreferenceByUri(uriString: String): UriPreference?

    /**
     * Saves or updates a URI preference. If a preference for the URI exists, it's updated.
     *
     * @param preference The UriPreference object to save.
     */
    suspend fun savePreference(preference: UriPreference)

    /**
     * Deletes the preference associated with a specific URI string.
     *
     * @param uriString The URI string whose preference should be removed.
     */
    suspend fun deletePreference(uriString: String)

    /**
     * Deletes all preferences associated with a given browser package name.
     * Useful if a browser is uninstalled.
     *
     * @param packageName The package name of the browser.
     * @return The number of preferences deleted.
     */
    suspend fun deletePreferencesForBrowser(packageName: String): Int
}

/**
 * Repository interface for retrieving information about installed browsers.
 * The implementation will likely interact with the Android PackageManager.
 */
interface BrowserRepository {

    /**
     * Retrieves a list of all installed applications that can handle web URIs (http/https).
     *
     * @return A list of BrowserApp objects representing available browsers.
     */
    suspend fun getAvailableBrowsers(): List<BrowserApp>

    /**
     * Gets the package name of the browser currently set as the system default.
     *
     * @return The package name of the default browser, or null if none is set or determinable.
     */
    suspend fun getDefaultBrowserPackageName(): String?
}
