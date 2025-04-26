package browserpicker.domain.model

import kotlinx.datetime.Instant

// Represents a single interaction with a URI, linked to a potential HostRule
data class UriRecord(
    val id: Long = 0,
    val uriString: String,
    val hostRuleId: Long?, // Nullable - record might exist without a rule
    val timestamp: Instant,
    val uriSource: UriSource,
    val interactionAction: InteractionAction,
    val chosenBrowserPackage: String? = null, // Package name of the chosen browser
)

// Represents a rule or preference applied to a specific host
data class HostRule(
    val id: Long = 0,
    val host: String,
    val ruleType: RuleType, // BOOKMARK, BLOCK, or NONE (if just preference is set)
    val bookmarkFolderId: Long? = null, // Null if ruleType is not BOOKMARK
    val blockFolderId: Long? = null, // Null if ruleType is not BLOCK
    val preferredBrowserPackage: String? = null, // Package name of preferred browser
    val isPreferenceEnabled: Boolean = true, // Whether the preference is active
    val createdAt: Instant,
    val updatedAt: Instant
)

// Represents a folder for organizing bookmarks or blocks
data class Folder(
    val id: Long = 0,
    val parentFolderId: Long? = null, // Null for root folders
    val name: String,
    val type: FolderType, // BOOKMARK or BLOCK
    val createdAt: Instant,
    val updatedAt: Instant
)
