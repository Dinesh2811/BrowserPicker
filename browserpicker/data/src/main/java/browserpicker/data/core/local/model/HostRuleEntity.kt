package browserpicker.data.core.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import browserpicker.domain.model.RuleType
import kotlinx.datetime.Instant

@Entity(
    tableName = "host_rules",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkFolderEntity::class,
            parentColumns = ["bookmark_folder_id"],
            childColumns = ["bookmark_folder_id"],
            onDelete = ForeignKey.SET_NULL, // If folder deleted, remove link but keep rule
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BlockFolderEntity::class,
            parentColumns = ["block_folder_id"],
            childColumns = ["block_folder_id"],
            onDelete = ForeignKey.SET_NULL, // If folder deleted, remove link but keep rule
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("host", unique = true), // Ensure host uniqueness
        Index("rule_type"),
        Index("bookmark_folder_id"), // Index for finding rules in a specific bookmark folder
        Index("block_folder_id")     // Index for finding rules in a specific block folder
    ]
    // CHECK constraint for folder_id based on rule_type is not directly supported in Room annotations.
    // This logic MUST be enforced in the Repository/Use Case layer during inserts/updates.
    // Ensure:
    // - If ruleType == BOOKMARK, blockFolderId MUST be null.
    // - If ruleType == BLOCK, bookmarkFolderId MUST be null.
    // - If ruleType == NONE, both folderIds MUST be null.
    // - ruleType should never be UNKNOWN in the database.
)
data class HostRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "host_id")
    val hostId: Long = 0,

    @ColumnInfo(name = "host")
    val host: String,

    @ColumnInfo(name = "rule_type", typeAffinity = ColumnInfo.INTEGER)
    val ruleType: RuleType, // BOOKMARK or BLOCK. Can't be UNKNOWN or NONE or 'null'

    @ColumnInfo(name = "bookmark_folder_id") // 'null' if ruleType is not BOOKMARK.
    val bookmarkFolderId: Long? = null,

    @ColumnInfo(name = "block_folder_id") // 'null' if ruleType is not BLOCK
    val blockFolderId: Long? = null,

    @ColumnInfo(name = "preferred_browser_package")
    val preferredBrowserPackage: String? = null,

    @ColumnInfo(name = "is_preference_enabled", defaultValue = "1")
    val isPreferenceEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
