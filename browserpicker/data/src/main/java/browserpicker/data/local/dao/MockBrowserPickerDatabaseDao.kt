package browserpicker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import browserpicker.data.local.entity.BrowserUsageStatEntity
import browserpicker.data.local.entity.FolderEntity
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.data.local.entity.UriRecordEntity

@Dao
interface MockBrowserPickerDatabaseDao {
    // --- UriRecordDao ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUriRecords(uriRecords: List<UriRecordEntity>)

    @Query("DELETE FROM uri_records")
    suspend fun deleteAllUriRecords()

    // --- FolderDao ---
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insertFoldersReturnIds(folders: List<FolderEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoldersIgnoreConflict(folders: List<FolderEntity>): List<Long>

    @Query("DELETE FROM folders") // Be careful using this! Only for debug/testing.
    suspend fun deleteAllFolders(): Int

    // ... HostRuleDao ...
    @Upsert
    suspend fun upsertHostRulesReturnIds(rules: List<HostRuleEntity>): List<Long> // Needed by generator

    @Query("DELETE FROM host_rules") // Careful!
    suspend fun deleteAllHostRules(): Int

    // ... UriRecordDao ...
    @Query("SELECT * FROM uri_records ORDER BY timestamp ASC") // Simple query for generator
    suspend fun getAllUriRecordsDebug(): List<UriRecordEntity>

    // ... BrowserUsageStatDao ...
    @Upsert
    suspend fun upsertBrowserUsageStats(stats: List<BrowserUsageStatEntity>)

    @Query("DELETE FROM browser_usage_stats")
    suspend fun deleteAllStats()
}
