package com.dinesh.playground.testing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long // Return row ID

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteById(folderId: String)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE name = :name AND type = :type AND parent_id = :parentId LIMIT 1")
    suspend fun findByNameAndParent(name: String, type: FolderType, parentId: String?): FolderEntity?

    @Query("SELECT * FROM folders WHERE name = :name AND type = :type AND parent_id IS NULL LIMIT 1")
    suspend fun findRootFolderByNameAndType(name: String, type: FolderType): FolderEntity? // Specific helper for root

    @Query("SELECT * FROM folders WHERE parent_id = :parentId ORDER BY name ASC")
    suspend fun findByParentId(parentId: String): List<FolderEntity> // For recursive deletion checks

    // Observe root folders, optionally filtered by type
    @Query("""
        SELECT * FROM folders
        WHERE parent_id IS NULL
        AND (:typeFilter IS NULL OR type = :typeFilter)
        ORDER BY name ASC
    """)
    fun observeRootFolders(typeFilter: FolderType?): Flow<List<FolderEntity>>

    // Observe child folders of a specific parent, optionally filtered by type
    @Query("""
        SELECT * FROM folders
        WHERE parent_id = :parentIdFilter
        AND (:typeFilter IS NULL OR type = :typeFilter)
        ORDER BY name ASC
    """)
    fun observeChildFolders(parentIdFilter: String, typeFilter: FolderType?): Flow<List<FolderEntity>>

    // Combined observe method (called by DataSource)
    fun observeAllFiltered(typeFilter: FolderType?, parentIdFilter: String?): Flow<List<FolderEntity>> {
        return if (parentIdFilter == null) {
            observeRootFolders(typeFilter)
        } else {
            observeChildFolders(parentIdFilter, typeFilter)
        }
    }

    // Internal raw queries used by observeAllFiltered - needed because @Query needs constant expressions
    @Query("""SELECT * FROM folders WHERE type = :typeFilter AND parent_id = :parentIdFilter ORDER BY name ASC""")
    fun observeByTypeAndParentInternal(typeFilter: FolderType, parentIdFilter: String): Flow<List<FolderEntity>>

    @Query("""SELECT * FROM folders WHERE type = :typeFilter AND parent_id IS NULL ORDER BY name ASC""")
    fun observeByTypeAndRootInternal(typeFilter: FolderType): Flow<List<FolderEntity>>

    @Query("""SELECT * FROM folders WHERE parent_id = :parentIdFilter ORDER BY name ASC""")
    fun observeByParentInternal(parentIdFilter: String): Flow<List<FolderEntity>>

    @Query("""SELECT * FROM folders WHERE parent_id IS NULL ORDER BY name ASC""")
    fun observeRootInternal(): Flow<List<FolderEntity>>

    // Actual calls with raw query string (Room KSP might handle this better, but rawQuery is an option)
    @androidx.room.RawQuery(observedEntities = [FolderEntity::class])
    fun observeByTypeAndParent(typeFilter: FolderType, parentIdFilter: String, queryString: String): Flow<List<FolderEntity>>
    @androidx.room.RawQuery(observedEntities = [FolderEntity::class])
    fun observeByTypeAndRoot(typeFilter: FolderType, queryString: String): Flow<List<FolderEntity>>
    @androidx.room.RawQuery(observedEntities = [FolderEntity::class])
    fun observeByParent(parentIdFilter: String, queryString: String): Flow<List<FolderEntity>>
    @androidx.room.RawQuery(observedEntities = [FolderEntity::class])
    fun observeRoot(queryString: String): Flow<List<FolderEntity>>

    // Simpler implementation if dynamic query building is complex or less performant:
    // Query without parent filter
    // @Query("SELECT * FROM folders WHERE (:typeFilter IS NULL OR type = :typeFilter) AND parent_id IS NULL ORDER BY name ASC")
    // fun observeRootFolders(typeFilter: FolderType?): Flow<List<FolderEntity>>
    // Query with parent filter
    // @Query("SELECT * FROM folders WHERE (:typeFilter IS NULL OR type = :typeFilter) AND parent_id = :parentIdFilter ORDER BY name ASC")
    // fun observeChildFolders(typeFilter: FolderType?, parentIdFilter: String): Flow<List<FolderEntity>>
}


@Dao
interface UriEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if URI already exists (based on unique index)
    suspend fun insert(uriEntry: UriEntryEntity): Long // Returns row ID, or -1 if ignored

    @Update
    suspend fun update(uriEntry: UriEntryEntity)

    // Optimized update for status/folder change
    @Query("""
        UPDATE uri_entries
        SET status = :newStatus, folder_id = :newFolderId, status_updated_at = :statusUpdatedAt
        WHERE uri_string = :uriString
    """)
    suspend fun updateStatusAndFolder(uriString: String, newStatus: UriStatus, newFolderId: String?, statusUpdatedAt: Instant)

    // Optimized update for just moving folder
    @Query("""
         UPDATE uri_entries
         SET folder_id = :newFolderId, status_updated_at = :statusUpdatedAt
         WHERE uri_string = :uriString AND status != :statusNone
     """) // Only update if bookmarked or blocked
    suspend fun updateFolder(uriString: String, newFolderId: String, statusUpdatedAt: Instant, statusNone: UriStatus = UriStatus.NONE)


    // Optimized update for last accessed time
    @Query("UPDATE uri_entries SET last_accessed_at = :lastAccessedAt WHERE uri_string = :uriString")
    suspend fun updateLastAccessedTime(uriString: String, lastAccessedAt: Instant)

    @Query("DELETE FROM uri_entries WHERE uri_string = :uriString")
    suspend fun deleteByUriString(uriString: String)

    // Bulk delete by folder ID
    @Query("DELETE FROM uri_entries WHERE folder_id = :folderId")
    suspend fun deleteByFolderId(folderId: String): Int // Returns number of rows affected

    // Bulk update folder ID
    @Query("""
        UPDATE uri_entries
        SET folder_id = :newFolderId, status_updated_at = :statusUpdatedAt, status = CASE WHEN :newFolderId IS NULL THEN :statusNone ELSE status END
        WHERE folder_id = :oldFolderId
    """)
    suspend fun updateFolderIdForEntries(oldFolderId: String, newFolderId: String?, statusUpdatedAt: Instant, statusNone: UriStatus = UriStatus.NONE): Int // Returns number of rows affected

    @Query("SELECT * FROM uri_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UriEntryEntity?

    @Query("SELECT * FROM uri_entries WHERE uri_string = :uriString LIMIT 1")
    suspend fun findByUriString(uriString: String): UriEntryEntity?

    // --- Observation Queries with Filtering ---

    // Observe simple UriEntryEntity
    @Transaction // Recommended for Flows returning complex objects or relations
    @Query("""
        SELECT * FROM uri_entries
        WHERE (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR uri_string LIKE '%' || :searchQuery || '%')
        ORDER BY intercepted_at DESC
    """)
    fun observeAllFiltered(statusFilter: UriStatus?, searchQuery: String?): Flow<List<UriEntryEntity>>

    // Observe UriEntryWithFolderEntity (using JOIN)
    @Transaction // Ensures consistency when joining
    @Query("""
        SELECT ue.*, f.*
        FROM uri_entries ue LEFT JOIN folders f ON ue.folder_id = f.id
        WHERE (:statusFilter IS NULL OR ue.status = :statusFilter)
        AND (:searchQuery IS NULL OR ue.uri_string LIKE '%' || :searchQuery || '%')
        ORDER BY ue.intercepted_at DESC
    """)
    fun observeAllWithFoldersFiltered(statusFilter: UriStatus?, searchQuery: String?): Flow<List<UriEntryWithFolderEntity>>
}


@Dao
interface UriPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Replace if preference for URI already exists
    suspend fun insert(preference: UriPreferenceEntity)

    @Query("DELETE FROM uri_preferences WHERE uri_string = :uriString")
    suspend fun deleteByUriString(uriString: String)

    // Bulk delete by package name
    @Query("DELETE FROM uri_preferences WHERE preferred_browser_package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String): Int // Returns number of rows affected

    @Query("SELECT * FROM uri_preferences WHERE uri_string = :uriString LIMIT 1")
    suspend fun findByUriString(uriString: String): UriPreferenceEntity?

    @Query("SELECT * FROM uri_preferences ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UriPreferenceEntity>>
}
