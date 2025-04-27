//package browserpicker.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import androidx.room.Update
//import browserpicker.data.local.entity.BookmarkFolderEntity
//import kotlinx.coroutines.flow.Flow
//import kotlinx.datetime.Instant
//
//@Dao
//interface BookmarkFolderDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(folder: BookmarkFolderEntity): Long
//
//    @Update
//    suspend fun update(folder: BookmarkFolderEntity): Int
//
//    @Query("DELETE FROM bookmark_folders WHERE bookmark_folder_id = :id")
//    suspend fun deleteById(id: Long): Int
//
//    @Query("SELECT * FROM bookmark_folders WHERE bookmark_folder_id = :id")
//    suspend fun getFolderById(id: Long): BookmarkFolderEntity?
//
//    // Observe all folders (potentially hierarchical, handle in Repository/UseCase)
//    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
//    fun observeAllFolders(): Flow<List<BookmarkFolderEntity>>
//
//    // Observe folders by parent ID (null for top-level)
//    @Query("SELECT * FROM bookmark_folders WHERE parent_bookmark_folder_id IS :parentId ORDER BY name ASC")
//    fun observeFoldersByParent(parentId: Long?): Flow<List<BookmarkFolderEntity>>
//
//    // Get all folders (non-Flow, potentially for initial hierarchy build)
//    @Query("SELECT * FROM bookmark_folders")
//    suspend fun getAllFolders(): List<BookmarkFolderEntity>
//
//    // Update parent ID when a parent folder is deleted (part of SET NULL logic)
//    @Query("UPDATE bookmark_folders SET parent_bookmark_folder_id = NULL, updated_at = :timestamp WHERE parent_bookmark_folder_id = :parentId")
//    suspend fun setChildrenParentToNull(parentId: Long, timestamp: Instant)
//}
