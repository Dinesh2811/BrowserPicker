package browserpicker.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UriRecord(
    val id: Long = 0,
    val uriString: String,
    val hostRuleId: Long?,
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
    val bookmarkFolderId: Long? = null,
    val blockFolderId: Long? = null,
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
