package browserpicker.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import androidx.core.net.toUri
import browserpicker.core.utils.logDebug
import browserpicker.core.utils.logInfo
import browserpicker.domain.service.UriParser

@Immutable @Serializable
data class UriRecord(
    val id: Long = 0,
    val uriString: String,
    val host: String,
    val associatedHostRuleId: Long? = null,
    val timestamp: Instant,
    val uriSource: UriSource = UriSource.INTENT,
    val interactionAction: InteractionAction,
    val chosenBrowserPackage: String? = null,
) {
    init {
        logDebug("$uriSource     $interactionAction", "log_UriRecord")
        if (uriSource == UriSource.INTENT || uriSource == UriSource.CLIPBOARD || uriSource == UriSource.MANUAL) {

        } else {
            logInfo("$uriSource", "log_UriRecord")
        }
        require(uriString.isNotBlank()) { "uriString must not be blank" }
        require(host.isNotBlank()) { "host must not be blank" }
        require(interactionAction != InteractionAction.UNKNOWN) { "interactionAction must be a valid type" }
        require(uriSource != UriSource.UNKNOWN) { "uriSource must be a valid type" }
//        require(uriSource == UriSource.INTENT || uriSource == UriSource.CLIPBOARD || uriSource == UriSource.MANUAL) { "uriSource must be a valid type" }
        if (chosenBrowserPackage != null) {
            require(chosenBrowserPackage.isNotBlank()) { "chosenBrowserPackage must not be blank if provided" }
        }
    }
}

@Immutable @Serializable
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
        if (uriStatus == UriStatus.NONE) require(folderId == null && preferredBrowserPackage == null && !isPreferenceEnabled) { "NONE status may not have folder, preference, or enabled preference" }
        if (uriStatus == UriStatus.BLOCKED) require(preferredBrowserPackage == null && !isPreferenceEnabled) { "BLOCKED status must not have preference" }
        if (preferredBrowserPackage != null) {
            require(preferredBrowserPackage.isNotBlank()) { "preferredBrowserPackage must not be blank if provided" }
        }
    }
}

@Immutable @Serializable
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
        require(type != FolderType.UNKNOWN) { "folder type must not be UNKNOWN" }
    }
    companion object {
        const val DEFAULT_BOOKMARK_ROOT_FOLDER_ID = 1L
        const val DEFAULT_BLOCKED_ROOT_FOLDER_ID = 2L
        const val DEFAULT_BOOKMARK_ROOT_FOLDER_NAME = "Bookmarks"
        const val DEFAULT_BLOCKED_ROOT_FOLDER_NAME = "Blocked"
    }
}

@Immutable @Serializable
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