package browserpicker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import browserpicker.data.local.entity.BlockFolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface BlockFolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: BlockFolderEntity): Long

    @Update
    suspend fun update(folder: BlockFolderEntity): Int

    @Query("DELETE FROM block_folders WHERE block_folder_id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM block_folders WHERE block_folder_id = :id")
    suspend fun getFolderById(id: Long): BlockFolderEntity?

    @Query("SELECT * FROM block_folders ORDER BY name ASC")
    fun observeAllFolders(): Flow<List<BlockFolderEntity>>

    @Query("SELECT * FROM block_folders WHERE parent_block_folder_id IS :parentId ORDER BY name ASC")
    fun observeFoldersByParent(parentId: Long?): Flow<List<BlockFolderEntity>>

    @Query("SELECT * FROM block_folders")
    suspend fun getAllFolders(): List<BlockFolderEntity>

    @Query("UPDATE block_folders SET parent_block_folder_id = NULL, updated_at = :timestamp WHERE parent_block_folder_id = :parentId")
    suspend fun setChildrenParentToNull(parentId: Long, timestamp: Instant)
}
