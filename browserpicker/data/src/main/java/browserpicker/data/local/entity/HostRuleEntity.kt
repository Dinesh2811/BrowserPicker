package browserpicker.data.local.entity

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import browserpicker.domain.model.UriStatus
import kotlinx.datetime.Instant

@Entity(
    tableName = "host_rules",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["folder_id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.SET_NULL, // If folder deleted, unlink but keep the rule
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index("host", unique = true), // Host must be unique
        Index("uri_status"),
        Index("folder_id"), // Link to the single folder table
        Index("is_preference_enabled"),
        Index("updated_at")
    ]
    // === Business Logic Constraints (Enforced in Repository/Use Case Layer) ===
    // 1. `uriStatus` MUST NOT be `UriStatus.UNKNOWN`.
    // 2. If `uriStatus` is `UriStatus.NONE`, `folder_id` MUST be null.
    // 3. If `uriStatus` is `UriStatus.BOOKMARKED`, `folder_id` MAY be non-null and MUST point to a FolderEntity with `type = FolderType.BOOKMARK`.
    // 4. If `uriStatus` is `UriStatus.BLOCKED`, `folder_id` MAY be non-null and MUST point to a FolderEntity with `type = FolderType.BLOCK`.
    // 5. If `uriStatus` is `UriStatus.BLOCKED`, `preferredBrowserPackage` and `isPreferenceEnabled` should ideally be cleared/ignored, but the DB schema allows them.
)
data class HostRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "host_rule_id")
    val id: Long = 0,

    @ColumnInfo(name = "host", collate = ColumnInfo.NOCASE) // Hosts are case-insensitive
    val host: String,

    @ColumnInfo(name = "uri_status") // Use non-null converter (NONE, BOOKMARKED, BLOCKED)
    val uriStatus: UriStatus,

    @ColumnInfo(name = "folder_id") // Single Folder ID (nullable)
    val folderId: Long? = null,

    @ColumnInfo(name = "preferred_browser_package")
    val preferredBrowserPackage: String? = null,

    @ColumnInfo(name = "is_preference_enabled", defaultValue = "1") // SQLite uses 1 for true
    val isPreferenceEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
