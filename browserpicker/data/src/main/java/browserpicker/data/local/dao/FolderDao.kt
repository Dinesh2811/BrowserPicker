package browserpicker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import browserpicker.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Upsert
    suspend fun upsertFolder(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoldersIgnoreConflict(folders: List<FolderEntity>): List<Long>

    @Query("SELECT * FROM folders WHERE folder_id = :id")
    fun getFolderById(id: Long): Flow<FolderEntity?>

    // Get direct children of a folder (parentFolderId matches)
    @Query("SELECT * FROM folders WHERE parent_folder_id = :parentFolderId ORDER BY name ASC")
    fun getChildFolders(parentFolderId: Long): Flow<List<FolderEntity>>

    // Get root folders (parentFolderId is NULL) of a specific type
    @Query("SELECT * FROM folders WHERE parent_folder_id IS NULL AND folder_type = :folderType ORDER BY name ASC")
    fun getRootFoldersByType(folderType: browserpicker.domain.model.FolderType): Flow<List<FolderEntity>>

    // Get all folders of a specific type
    @Query("SELECT * FROM folders WHERE folder_type = :folderType ORDER BY name ASC")
    fun getAllFoldersByType(folderType: browserpicker.domain.model.FolderType): Flow<List<FolderEntity>>

    // Check for uniqueness constraint: name + parent + type
    @Query("SELECT * FROM folders WHERE name = :name AND parent_folder_id = :parentFolderId AND folder_type = :folderType LIMIT 1")
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, folderType: browserpicker.domain.model.FolderType): FolderEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE parent_folder_id = :folderId LIMIT 1)")
    suspend fun hasChildFolders(folderId: Long): Boolean

    @Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @Query("DELETE FROM folders WHERE folder_id = :id")
    suspend fun deleteFolderById(id: Long): Int

    // Get all folder IDs (potentially for cleanup or validation)
    @Query("SELECT folder_id FROM folders")
    suspend fun getAllFolderIds(): List<Long>
}