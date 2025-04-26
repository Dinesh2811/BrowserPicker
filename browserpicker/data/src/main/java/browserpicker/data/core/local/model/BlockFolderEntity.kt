package browserpicker.data.core.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "block_folders",
    foreignKeys = [
        ForeignKey(
            entity = BlockFolderEntity::class,
            parentColumns = ["block_folder_id"],
            childColumns = ["parent_block_folder_id"],
            onDelete = ForeignKey.SET_NULL, // If parent deleted, make this a top-level folder
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parent_block_folder_id")
    ]
)
data class BlockFolderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "block_folder_id")
    val blockFolderId: Long = 0,

    /** Null indicates a top-level folder. */
    @ColumnInfo(name = "parent_block_folder_id", index = true)
    val parentBookmarkFolderId: Long? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
