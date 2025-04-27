//package browserpicker.data.local.entity
//
//import androidx.annotation.Keep
//import androidx.compose.runtime.Immutable
//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.ForeignKey
//import androidx.room.Index
//import androidx.room.PrimaryKey
//import kotlinx.datetime.Instant
//
//@Entity(
//    tableName = "block_folders",
//    foreignKeys = [
//        ForeignKey(
//            entity = BlockFolderEntity::class,
//            parentColumns = ["block_folder_id"],
//            childColumns = ["parent_block_folder_id"],
//            onDelete = ForeignKey.SET_NULL, // If parent deleted, make this a top-level folder
//            onUpdate = ForeignKey.CASCADE,
//            deferred = true
//        ),
//    ],
//    indices = [
//        Index("parent_block_folder_id"),
//        Index("name")
//    ]
//)
//data class BlockFolderEntity(
//    @PrimaryKey(autoGenerate = true)
//    @ColumnInfo(name = "block_folder_id")
//    val id: Long = 0,
//
//    @ColumnInfo(name = "parent_block_folder_id") // Null indicates a top-level folder
//    val parentFolderId: Long? = null,
//
//    @ColumnInfo(name = "name")
//    val name: String,
//
//    @ColumnInfo(name = "created_at")
//    val createdAt: Instant,
//
//    @ColumnInfo(name = "updated_at")
//    val updatedAt: Instant
//)
//
//
//@Entity(
//    tableName = "bookmark_folders",
//    foreignKeys = [
//        ForeignKey(
//            entity = BookmarkFolderEntity::class,
//            parentColumns = ["bookmark_folder_id"],
//            childColumns = ["parent_bookmark_folder_id"],
//            onDelete = ForeignKey.SET_NULL, // If parent deleted, make this a top-level folder
//            onUpdate = ForeignKey.CASCADE,
//            deferred = true
//        )
//    ],
//    indices = [
//        Index("parent_bookmark_folder_id"),
//        Index("name")
//    ]
//)
//data class BookmarkFolderEntity(
//    @PrimaryKey(autoGenerate = true)
//    @ColumnInfo(name = "bookmark_folder_id")
//    val id: Long = 0,
//
//    @ColumnInfo(name = "parent_bookmark_folder_id") // Null indicates a top-level folder
//    val parentFolderId: Long? = null,
//
//    @ColumnInfo(name = "name")
//    val name: String,
//
//    @ColumnInfo(name = "created_at")
//    val createdAt: Instant,
//
//    @ColumnInfo(name = "updated_at")
//    val updatedAt: Instant
//)
