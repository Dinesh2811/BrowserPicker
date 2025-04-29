package browserpicker.playground.browserpicker.data

import browserpicker.playground.browserpicker.domain.*
import androidx.room.*
import kotlinx.datetime.Instant

@Entity(
    tableName = "uri_records",
    foreignKeys = [
        ForeignKey(
            entity = HostRuleEntity::class,
            parentColumns = ["host_rule_id"],
            childColumns = ["associated_host_rule_id"],
            onDelete = ForeignKey.SET_NULL, // Keep history even if rule deleted
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index("associated_host_rule_id"),
        Index("host"),
        Index("timestamp"),
        Index("uri_string"),
        Index("interaction_action"),
        Index("uri_source"),
        Index("chosen_browser_package")
    ]
)
data class UriRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uri_record_id")
    val id: Long = 0,

    @ColumnInfo(name = "uri_string", collate = ColumnInfo.NOCASE)
    val uriString: String,

    @ColumnInfo(name = "host", collate = ColumnInfo.NOCASE)
    val host: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Instant,

    @ColumnInfo(name = "uri_source", typeAffinity = ColumnInfo.INTEGER)
    val uriSource: Int, /** [UriSource] */

    @ColumnInfo(name = "interaction_action", typeAffinity = ColumnInfo.INTEGER)
    val interactionAction: Int, /** [InteractionAction] */

    @ColumnInfo(name = "chosen_browser_package")
    val chosenBrowserPackage: String? = null,

    @ColumnInfo(name = "associated_host_rule_id")
    val associatedHostRuleId: Long?
)

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
    val uriStatus: Int, /** [UriStatus] */

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

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class, // Self-referencing for parent folder
            parentColumns = ["folder_id"],
            childColumns = ["parent_folder_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index("parent_folder_id"),
        Index("name"),
        Index("folder_type"),
        Index(value = ["parent_folder_id", "name", "folder_type"], unique = true)
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "folder_id")
    val id: Long = 0,

    @ColumnInfo(name = "parent_folder_id") // Null indicates top-level folder
    val parentFolderId: Long? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "folder_type", typeAffinity = ColumnInfo.INTEGER)
    val folderType: Int, /** [FolderType] */

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)

@Entity(
    tableName = "browser_usage_stats",
    indices = [
        Index(value = ["last_used_timestamp"]),
        Index(value = ["usage_count"])
    ]
)
data class BrowserUsageStatEntity(
    @PrimaryKey
    @ColumnInfo(name = "browser_package_name")
    val browserPackageName: String,

    @ColumnInfo(name = "usage_count", defaultValue = "0")
    val usageCount: Long = 0,

    @ColumnInfo(name = "last_used_timestamp")
    val lastUsedTimestamp: Instant
)
