package com.dinesh.playground.testing

import kotlinx.datetime.Instant

/**
 * Represents the possible states of a URI managed by the app.
 * A URI can either be Bookmarked or Blocked, but not both.
 * NONE indicates it's tracked but has no specific user-defined status.
 * BLOCK takes precedence over BOOKMARKED.
 */
enum class UriStatus {
    NONE,       // Default state for a newly intercepted or managed URI
    BOOKMARKED, // URI is explicitly saved by the user for later access
    BLOCKED     // URI is explicitly blocked by the user
}

/**
 * Represents the source from which a URI was intercepted or added.
 */
enum class UriSource {
    INTENT,     // Intercepted via Intent.ACTION_VIEW
    CLIPBOARD,  // Added from the system clipboard
    MANUAL,     // Manually entered or added by the user within the app
    UNKNOWN     // Source could not be determined
}

/**
 * Represents the type of folder, differentiating between bookmark folders and blocklist folders.
 */
enum class FolderType {
    BOOKMARK,
    BLOCK
}

/**
 * Represents a Folder for organizing URIs. Folders can be nested.
 *
 * @property id Unique identifier for the folder (e.g., UUID).
 * @property name User-defined name of the folder.
 * @property parentId Identifier of the parent folder, null if it's a root folder.
 * @property type The type of URIs this folder contains (Bookmark or Block).
 * @property createdAt Timestamp when the folder was created.
 * @property updatedAt Timestamp when the folder was last updated.
 */
data class Folder(
    val id: String,
    val name: String,
    val parentId: String?, // Null indicates root folder
    val type: FolderType,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Represents a single URI entry tracked by the application.
 *
 * @property id Unique identifier for this entry (e.g., UUID).
 * @property uriString The actual URI string (e.g., "https://www.example.com").
 * @property status The current status (None, Bookmarked, Blocked).
 * @property folderId The ID of the Folder this URI belongs to, if Bookmarked or Blocked. Null otherwise.
 * @property source How this URI was added to the app.
 * @property interceptedAt Timestamp when the URI was first intercepted or added.
 * @property lastAccessedAt Timestamp when the URI was last opened or accessed via the app. Can be null.
 * @property statusUpdatedAt Timestamp when the status (or folder) was last changed.
 */
data class UriEntry(
    val id: String,
    val uriString: String,
    val status: UriStatus,
    val folderId: String?, // FK to Folder.id, null if status is NONE
    val source: UriSource,
    val interceptedAt: Instant,
    val lastAccessedAt: Instant?,
    val statusUpdatedAt: Instant
) {
    init {
        // Ensure consistency: folderId must be non-null if status is Bookmarked or Blocked,
        // and null if status is None.
        require((status == UriStatus.BOOKMARKED || status == UriStatus.BLOCKED) == (folderId != null)) {
            "folderId must be non-null if and only if status is BOOKMARKED or BLOCKED. Status: $status, FolderId: $folderId"
        }
    }
}

/**
 * Represents an installed application capable of handling web URIs (a browser).
 *
 * @property packageName The package name of the browser application (e.g., "com.android.chrome").
 * @property activityName The specific Activity class name within the package that handles VIEW intents.
 * @property userFriendlyName The human-readable name of the browser (e.g., "Chrome").
 * @property isDefaultBrowser Indicates if this browser is currently set as the system's default browser.
 */
data class BrowserApp(
    val packageName: String,
    val activityName: String,
    val userFriendlyName: String,
    val isDefaultBrowser: Boolean
    // Note: Icon handling is a presentation concern, so we don't include Drawable IDs here.
    // The presentation layer will resolve icons based on packageName/activityName.
)

/**
 * Represents a user preference to always open a specific URI (or potentially a pattern in the future)
 * with a particular browser.
 *
 * @property id Unique identifier for the preference (e.g., UUID).
 * @property uriString The exact URI string this preference applies to.
 * @property preferredBrowserPackageName The package name of the browser selected for this URI.
 * @property createdAt Timestamp when the preference was created.
 */
data class UriPreference(
    val id: String,
    val uriString: String, // For now, exact match. Could be expanded to patterns later.
    val preferredBrowserPackageName: String, // References BrowserApp.packageName
    val createdAt: Instant
)

/**
 * Helper model to potentially bundle related info for display or processing.
 * For example, showing a URI entry along with its associated folder name.
 * (This might be defined later or in specific use cases/viewmodels if needed,
 * but defining it here shows forethought about common data combinations).
 *
 * @property uriEntry The core UriEntry object.
 * @property folder The Folder object associated with the uriEntry, if applicable.
 */
data class UriEntryWithFolder(
    val uriEntry: UriEntry,
    val folder: Folder? // Null if uriEntry.status is NONE
)
