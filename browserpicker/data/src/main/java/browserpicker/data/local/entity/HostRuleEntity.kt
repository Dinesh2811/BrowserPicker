package browserpicker.data.local.entity

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
        Index("host", unique = true),
        Index("uri_status"),
        Index("folder_id"),
        Index("is_preference_enabled"),
        Index("updated_at")
    ]
    // === Business Logic Constraints (MUST be enforced in Repository/Use Case Layer) ===
    // 1. `uriStatus` MUST NOT be `UriStatus.UNKNOWN` when saving. Default to `NONE` if unset.
    // 2. If `uriStatus` is `UriStatus.NONE`, `folder_id` MUST be null.
    // 3. If `uriStatus` is `UriStatus.BOOKMARKED`, `folder_id` MAY be non-null and MUST point to a FolderEntity with `folder_type = FolderType.BOOKMARK`. If null, implies root bookmark folder.
    // 4. If `uriStatus` is `UriStatus.BLOCKED`, `folder_id` MAY be non-null and MUST point to a FolderEntity with `folder_type = FolderType.BLOCK`. If null, implies root block folder.
    // 5. If `uriStatus` is set to `UriStatus.BLOCKED`, `preferredBrowserPackage` MUST be set to null and `isPreferenceEnabled` MUST be set to false.
    // 6. If `uriStatus` is set to `UriStatus.NONE`, `folder_id` MUST be set to null. (Redundant with 2, but emphasizes cleanup)
    // 7. `folderId` must reference a FolderEntity whose `folderType` matches the intent implied by `uriStatus` (BOOKMARK for BOOKMARKED, BLOCK for BLOCKED).
)
data class HostRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "host_rule_id")
    val id: Long = 0,

    @ColumnInfo(name = "host", collate = ColumnInfo.NOCASE)
    val host: String,

    @ColumnInfo(name = "uri_status", typeAffinity = ColumnInfo.INTEGER)
    val uriStatus: UriStatus,

    @ColumnInfo(name = "folder_id")
    val folderId: Long? = null,

    @ColumnInfo(name = "preferred_browser_package")
    val preferredBrowserPackage: String? = null,

    @ColumnInfo(name = "is_preference_enabled", defaultValue = "1")
    val isPreferenceEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
