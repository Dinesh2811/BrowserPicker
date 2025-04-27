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
            entity = BookmarkFolderEntity::class,
            parentColumns = ["bookmark_folder_id"],
            childColumns = ["bookmark_folder_id"],
            onDelete = ForeignKey.SET_NULL, // If folder deleted, remove link but keep rule
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = BlockFolderEntity::class,
            parentColumns = ["block_folder_id"],
            childColumns = ["block_folder_id"],
            onDelete = ForeignKey.SET_NULL, // If folder deleted, remove link but keep rule
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        ),
    ],
    indices = [
        Index("host", unique = true),
        Index("rule_type"),
        Index("bookmark_folder_id"),
        Index("block_folder_id"),
        Index("is_preference_enabled"),
        Index("updated_at")
    ]
    // IMPORTANT CONSTRAINT (Enforced in Repository/Use Case Layer):
    // - If ruleType == BOOKMARK, blockFolderId MUST be null.
    // - If ruleType == BLOCK, bookmarkFolderId MUST be null.
    // - If ruleType == NONE, both folderIds MUST be null.
    // - ruleType should never be UNKNOWN in the database.
)
data class HostRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "host_rule_id")
    val id: Long = 0,

    @ColumnInfo(name = "host")
    val host: String,

    @ColumnInfo(name = "rule_type", typeAffinity = ColumnInfo.INTEGER)
    val uriStatus: UriStatus, // BOOKMARK or BLOCK. Can't be UNKNOWN or NONE or 'null'

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
