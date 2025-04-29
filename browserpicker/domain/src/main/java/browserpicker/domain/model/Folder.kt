package browserpicker.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import androidx.core.net.toUri

@Serializable
data class UriRecord(
    val id: Long = 0,
    val uriString: String,
    val host: String,
    val associatedHostRuleId: Long? = null,
    val timestamp: Instant,
    val uriSource: UriSource,
    val interactionAction: InteractionAction,
    val chosenBrowserPackage: String? = null,
) {
    init {
        require(uriString.isNotBlank()) { "uriString must not be blank" }
        require(isValidUri(uriString)) { "uriString must be a valid URI" }
        require(host.isNotBlank()) { "host must not be blank" }
    }

    companion object {
        fun isValidUri(uri: String): Boolean {
            return try {
                uri.toUri().isAbsolute
            } catch (e: Exception) {
                false
            }
        }
    }
}

@Serializable
data class HostRule(
    val id: Long = 0,
    val host: String,
    val uriStatus: UriStatus,
    val folderId: Long? = null,
    val preferredBrowserPackage: String? = null,
    val isPreferenceEnabled: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(host.isNotBlank()) { "host must not be blank" }
        require(uriStatus != UriStatus.UNKNOWN) { "uriStatus must not be UNKNOWN" }
        if (uriStatus == UriStatus.NONE) require(folderId == null && preferredBrowserPackage == null) { "NONE status may not have folder or preference" }
        if (uriStatus == UriStatus.BLOCKED) require(preferredBrowserPackage == null && !isPreferenceEnabled) { "BLOCKED status must not have preference" }
    }
}

@Serializable
data class Folder(
    val id: Long = 0,
    val parentFolderId: Long? = null,
    val name: String,
    val type: FolderType,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "folder name must not be blank" }
        require(createdAt <= updatedAt) { "createdAt must not be after updatedAt" }
    }
    companion object {
        const val DEFAULT_BOOKMARK_ROOT_FOLDER_ID = 1L
        const val DEFAULT_BLOCKED_ROOT_FOLDER_ID = 2L
        const val DEFAULT_BOOKMARK_ROOT_FOLDER_NAME = "Bookmarks"
        const val DEFAULT_BLOCKED_ROOT_FOLDER_NAME = "Blocked"
    }
}

@Serializable
data class BrowserUsageStat(
    val browserPackageName: String,
    val usageCount: Long,
    val lastUsedTimestamp: Instant,
) {
    init {
        require(browserPackageName.isNotBlank()) { "browserPackageName must not be blank" }
        require(usageCount >= 0) { "usageCount must be non-negative" }
    }
}