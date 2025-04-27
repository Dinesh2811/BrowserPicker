package browserpicker.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
)

@Serializable
data class HostRule(
    val id: Long = 0,
    val host: String,
    val uriStatus: UriStatus,
    val folderId: Long? = null,
    val preferredBrowserPackage: String? = null,
    val isPreferenceEnabled: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class Folder(
    val id: Long = 0,
    val parentFolderId: Long? = null,
    val name: String,
    val type: FolderType,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class BrowserUsageStat(
    val browserPackageName: String,
    val usageCount: Long,
    val lastUsedTimestamp: Instant
)