package com.dinesh.playground.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock // Assuming Clock is injected via DI
import javax.inject.Inject // Using javax.inject for DI annotations


// --- URI Management Use Cases ---

/**
 * Use Case to handle the initial interception or addition of a URI.
 * It checks for existing entries, preferences, and block status.
 * Returns information needed by the UI to decide the next step (show picker, open directly, show blocked message).
 */
class ProcessIncomingUriUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val uriPreferenceRepository: UriPreferenceRepository,
    private val clock: Clock // Injected Clock
) {
    sealed class Result {
        data class OpenDirectly(val browserPackageName: String) : Result()
        data class ShowPicker(val uri: String) : Result()
        data object Blocked : Result()
        data class RequiresSaving(val uriEntry: UriEntry) : Result() // If new URI, save first
    }

    suspend operator fun invoke(uriString: String, source: UriSource): Result {
        // 1. Check if Blocked
        val existingEntry = uriHistoryRepository.findByUriString(uriString)
        if (existingEntry?.status == UriStatus.BLOCKED) {
            uriHistoryRepository.updateLastAccessedTime(uriString) // Update access time even if blocked
            return Result.Blocked
        }

        // 2. Check Preference
        val preference = uriPreferenceRepository.findPreferenceByUri(uriString)
        if (preference != null) {
            if (existingEntry != null) {
                uriHistoryRepository.updateLastAccessedTime(uriString)
            } else {
                // If preference exists but URI isn't in history (e.g., history cleared), add it now
                val newEntry = UriEntry(
                    id = generateUUID(), // Assume a UUID generator is available/injected
                    uriString = uriString,
                    status = UriStatus.NONE,
                    folderId = null,
                    source = source,
                    interceptedAt = clock.now(),
                    lastAccessedAt = clock.now(),
                    statusUpdatedAt = clock.now()
                )
                uriHistoryRepository.saveUriEntry(newEntry)
            }
            return Result.OpenDirectly(preference.preferredBrowserPackageName)
        }

        // 3. URI exists but no preference and not blocked? Or new URI?
        if (existingEntry != null) {
            // URI known, not blocked, no preference -> show picker
            uriHistoryRepository.updateLastAccessedTime(uriString)
            return Result.ShowPicker(uriString)
        } else {
            // New URI -> Needs saving first, then show picker
            val newEntry = UriEntry(
                id = generateUUID(),
                uriString = uriString,
                status = UriStatus.NONE,
                folderId = null,
                source = source,
                interceptedAt = clock.now(),
                lastAccessedAt = clock.now(), // Mark as accessed on first interception
                statusUpdatedAt = clock.now()
            )
            // The caller (e.g., ViewModel) should call saveUriEntry(newEntry)
            // and then proceed to show the picker.
            // Alternatively, save here and return ShowPicker. Let's save here for simplicity.
            uriHistoryRepository.saveUriEntry(newEntry)
            return Result.ShowPicker(uriString)
        }
    }
    // Placeholder for actual UUID generation
    private fun generateUUID(): String = java.util.UUID.randomUUID().toString()
}

class ObserveUriHistoryUseCase @Inject constructor(
    private val repository: UriHistoryRepository
) {
    operator fun invoke(
        statusFilter: UriStatus? = null,
        searchQuery: String? = null
    ): Flow<List<UriEntryWithFolder>> {
        // Prefer returning combined data if folder names are often needed
        return repository.observeUriHistoryWithFolders(statusFilter, searchQuery)
    }
}

class GetUriDetailsUseCase @Inject constructor(
    private val repository: UriHistoryRepository
) {
    suspend operator fun invoke(uriString: String): UriEntry? {
        return repository.findByUriString(uriString)
        // Could also fetch UriEntryWithFolder if needed
    }
}

class BookmarkUriUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val folderRepository: FolderRepository,
    private val uriPreferenceRepository: UriPreferenceRepository
) {
    suspend operator fun invoke(uriString: String, targetFolderId: String?) {
        val folderId = targetFolderId ?: folderRepository.getDefaultFolderId(FolderType.BOOKMARK)
        // Ensure the folder exists (optional, repo impl might handle this)
        require(folderRepository.getFolderById(folderId) != null) { "Target folder $folderId not found" }

        // Block takes precedence, if currently blocked, cannot bookmark directly.
        // UI should prevent this, but add check here for safety.
        val currentEntry = uriHistoryRepository.findByUriString(uriString)
        if (currentEntry?.status == UriStatus.BLOCKED) {
            // Or throw an exception / return a status
            println("Warning: Cannot bookmark a currently blocked URI: $uriString")
            return
        }

        // Remove preference if it exists, as bookmarking implies user interaction is okay
        uriPreferenceRepository.deletePreference(uriString)

        uriHistoryRepository.updateUriStatus(uriString, UriStatus.BOOKMARKED, folderId)
    }
}

class BlockUriUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val folderRepository: FolderRepository,
    private val uriPreferenceRepository: UriPreferenceRepository
) {
    suspend operator fun invoke(uriString: String, targetFolderId: String?) {
        val folderId = targetFolderId ?: folderRepository.getDefaultFolderId(FolderType.BLOCK)
        require(folderRepository.getFolderById(folderId) != null) { "Target folder $folderId not found" }

        // Block takes precedence, so always remove any existing preference.
        uriPreferenceRepository.deletePreference(uriString)

        // Update status to Blocked and assign to folder.
        uriHistoryRepository.updateUriStatus(uriString, UriStatus.BLOCKED, folderId)
    }
}

class UnmanageUriUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) {
    // Reverts a URI back to NONE status, removing it from any bookmark/block folder.
    suspend operator fun invoke(uriString: String) {
        uriHistoryRepository.updateUriStatus(uriString, UriStatus.NONE, null)
    }
}


class UpdateUriFolderUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val folderRepository: FolderRepository,
) {
    suspend operator fun invoke(uriString: String, newFolderId: String) {
        // Validate the target folder exists and has the correct type
        val entry = uriHistoryRepository.findByUriString(uriString)
        require(entry != null && entry.status != UriStatus.NONE) { "URI not found or has no status to change folder for." }
        val folder = folderRepository.getFolderById(newFolderId)
        require(folder != null) { "Target folder $newFolderId not found." }
        val expectedFolderType = if (entry.status == UriStatus.BOOKMARKED) FolderType.BOOKMARK else FolderType.BLOCK
        require(folder.type == expectedFolderType) { "Cannot move URI with status ${entry.status} to folder of type ${folder.type}"}

        uriHistoryRepository.updateUriFolder(uriString, newFolderId)
    }
}


class DeleteUriUseCase @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val uriPreferenceRepository: UriPreferenceRepository
) {
    // Deletes URI entry permanently
    suspend operator fun invoke(uriString: String) {
        // Also remove any associated preference
        uriPreferenceRepository.deletePreference(uriString)
        uriHistoryRepository.deleteUriEntry(uriString)
    }
}


// --- Folder Management Use Cases ---

class CreateFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(name: String, type: FolderType, parentId: String?): Result<Folder> {
        // Check for duplicates within the same parent/type
        if (folderRepository.findFolder(name, type, parentId) != null) {
            return Result.failure(IllegalArgumentException("Folder '$name' already exists."))
        }
        // Validate parent if provided
        if (parentId != null) {
            val parentFolder = folderRepository.getFolderById(parentId)
            require(parentFolder != null) { "Parent folder $parentId not found." }
            require(parentFolder.type == type) { "Parent folder type (${parentFolder.type}) must match new folder type ($type)." }
        }

        val newFolder = Folder(
            id = generateUUID(), // ID generated here or in repo impl
            name = name.trim(),
            parentId = parentId,
            type = type,
            createdAt = clock.now(),
            updatedAt = clock.now()
        )
        val created = folderRepository.createFolder(newFolder)
        return Result.success(created)
    }
    private fun generateUUID(): String = java.util.UUID.randomUUID().toString()
}

class ObserveFoldersUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(
        typeFilter: FolderType? = null,
        parentIdFilter: String? = null
    ): Flow<List<Folder>> {
        return folderRepository.observeFolders(typeFilter, parentIdFilter)
    }
}


class UpdateFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(folderId: String, newName: String?, newParentId: String?): Result<Unit> {
        val folder = folderRepository.getFolderById(folderId)
            ?: return Result.failure(IllegalArgumentException("Folder $folderId not found."))

        // Prevent moving folder into itself or its own descendants (more complex check needed for full hierarchy)
        if (newParentId == folderId) {
            return Result.failure(IllegalArgumentException("Cannot move folder into itself."))
        }
        // Basic check: prevent moving to a direct child (needs recursive check for full safety)
        if (newParentId != null) {
            val potentialChild = folderRepository.observeFolders(parentIdFilter = folderId).firstOrNull()?.find { it.id == newParentId }
            if(potentialChild != null) {
                return Result.failure(IllegalArgumentException("Cannot move folder into its own child folder."))
            }
            // Also validate new parent exists and type matches
            val newParent = folderRepository.getFolderById(newParentId)
                ?: return Result.failure(IllegalArgumentException("New parent folder $newParentId not found."))
            if (newParent.type != folder.type) {
                return Result.failure(IllegalArgumentException("Cannot move folder to a parent of different type."))
            }
        }

        // Check for name collision in the new location
        val finalName = newName?.trim() ?: folder.name
        val finalParentId = newParentId ?: folder.parentId // Use explicit newParentId if provided, otherwise keep old one
        val potentialCollision = folderRepository.findFolder(finalName, folder.type, finalParentId)
        if (potentialCollision != null && potentialCollision.id != folderId) {
            return Result.failure(IllegalArgumentException("A folder named '$finalName' already exists in the target location."))
        }


        val updatedFolder = folder.copy(
            name = finalName,
            parentId = finalParentId, // Allow changing parent
            updatedAt = clock.now()
        )
        folderRepository.updateFolder(updatedFolder)
        return Result.success(Unit)
    }
}

class DeleteFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository,
    private val uriHistoryRepository: UriHistoryRepository
) {
    // Defines behavior when deleting a non-empty folder
    enum class DeletionBehavior {
        DELETE_CONTENTS,      // Delete all URIs within the folder
        UNASSIGN_CONTENTS     // Move URIs to NONE status (remove folderId)
        // Could add MOVE_TO_DEFAULT or MOVE_TO_PARENT later
    }

    suspend operator fun invoke(folderId: String, behavior: DeletionBehavior) {
        // 1. Handle nested folders (recursive deletion) - Basic implementation, assumes hierarchy isn't too deep for simple recursion
        val childFolders = folderRepository.observeFolders(parentIdFilter = folderId).firstOrNull() ?: emptyList()
        for (child in childFolders) {
            invoke(child.id, behavior) // Recursively delete children first
        }

        // 2. Handle URIs within this folder
        when (behavior) {
            DeletionBehavior.DELETE_CONTENTS -> {
                uriHistoryRepository.deleteUriEntriesByFolderId(folderId)
            }
            DeletionBehavior.UNASSIGN_CONTENTS -> {
                // Set status to NONE and folderId to null for URIs in this folder
                val urisToUpdate = uriHistoryRepository.observeUriHistory().firstOrNull()?.filter { it.folderId == folderId } ?: emptyList()
                for (uri in urisToUpdate) {
                    uriHistoryRepository.updateUriStatus(uri.uriString, UriStatus.NONE, null)
                }
                // Alternative: A single repo call if `moveUriEntriesToFolder` supports setting folderId to null
                // uriHistoryRepository.moveUriEntriesToFolder(folderId, null)
            }
        }

        // 3. Delete the folder itself
        folderRepository.deleteFolder(folderId)
    }
}


// --- Preference Management Use Cases ---

class SetUriPreferenceUseCase @Inject constructor(
    private val uriPreferenceRepository: UriPreferenceRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(uriString: String, browserPackageName: String) {
        // Ensure URI exists in history (or add it) before setting preference
        val entry = uriHistoryRepository.findByUriString(uriString)
        if (entry == null) {
            // Or should we throw error? Decide based on desired app flow.
            // Let's assume we add it silently if missing.
            val newEntry = UriEntry(
                id = generateUUID(),
                uriString = uriString,
                status = UriStatus.NONE, // Preference is separate from status
                folderId = null,
                source = UriSource.UNKNOWN, // Source might be unknown when setting preference directly
                interceptedAt = clock.now(),
                lastAccessedAt = null,
                statusUpdatedAt = clock.now()
            )
            uriHistoryRepository.saveUriEntry(newEntry)
        } else {
            // If setting a preference for a blocked URI, unblock it first.
            // Preference implies user wants to open it.
            if (entry.status == UriStatus.BLOCKED) {
                uriHistoryRepository.updateUriStatus(uriString, UriStatus.NONE, null)
            }
        }


        val preference = UriPreference(
            id = generateUUID(), // ID generated here or repo impl
            uriString = uriString,
            preferredBrowserPackageName = browserPackageName,
            createdAt = clock.now()
        )
        uriPreferenceRepository.savePreference(preference)
    }
    private fun generateUUID(): String = java.util.UUID.randomUUID().toString()
}

class GetUriPreferenceUseCase @Inject constructor(
    private val uriPreferenceRepository: UriPreferenceRepository
) {
    suspend operator fun invoke(uriString: String): UriPreference? {
        return uriPreferenceRepository.findPreferenceByUri(uriString)
    }
}

class RemoveUriPreferenceUseCase @Inject constructor(
    private val uriPreferenceRepository: UriPreferenceRepository
) {
    suspend operator fun invoke(uriString: String) {
        uriPreferenceRepository.deletePreference(uriString)
    }
}

class ObservePreferencesUseCase @Inject constructor(
    private val repository: UriPreferenceRepository
) {
    operator fun invoke(): Flow<List<UriPreference>> {
        return repository.observeAllPreferences()
    }
}


// --- Browser Information Use Cases ---

class GetAvailableBrowsersUseCase @Inject constructor(
    private val browserRepository: BrowserRepository
) {
    suspend operator fun invoke(): List<BrowserApp> {
        return browserRepository.getAvailableBrowsers()
    }
}

class GetDefaultBrowserUseCase @Inject constructor(
    private val browserRepository: BrowserRepository
) {
    suspend operator fun invoke(): Flow<BrowserApp?> {
        // Combine getting all browsers and the default package name
        // Emit null initially or if no default found
        return kotlinx.coroutines.flow.flow {
            val defaultPackage = browserRepository.getDefaultBrowserPackageName()
            if (defaultPackage == null) {
                emit(null)
                return@flow
            }
            val browsers = browserRepository.getAvailableBrowsers()
            emit(browsers.find { it.packageName == defaultPackage })
        }
        // Note: This might be better implemented by observing system default changes if possible,
        // but that often requires broadcast receivers, pushing complexity to the data layer.
        // A simple suspend function returning BrowserApp? might be sufficient if observing isn't critical.
        // Let's change to simple suspend for now, can be enhanced later.
    }
    suspend fun get(): BrowserApp? {
        val defaultPackage = browserRepository.getDefaultBrowserPackageName() ?: return null
        return browserRepository.getAvailableBrowsers().find { it.packageName == defaultPackage }
    }
}
