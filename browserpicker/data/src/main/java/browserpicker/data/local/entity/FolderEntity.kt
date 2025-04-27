package browserpicker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import browserpicker.domain.model.FolderType
import kotlinx.datetime.Instant

@Entity(
    tableName = "folders", // Unified folder table
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class, // Self-referencing for parent folder
            parentColumns = ["folder_id"],
            childColumns = ["parent_folder_id"],
            onDelete = ForeignKey.SET_NULL, // Make orphaned folders top-level
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index("parent_folder_id"),
        Index("name"),
        Index("type") // Index on type for filtering bookmark/block folders
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

    @ColumnInfo(name = "type") // Use non-null converter (BOOKMARK or BLOCK)
    val type: FolderType,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)

