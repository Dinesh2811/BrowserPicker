package browserpicker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.data.local.entity.UriRecordEntity
import browserpicker.data.local.query.model.DateCount
import browserpicker.data.local.query.model.GroupCount
import kotlinx.coroutines.flow.Flow

@Dao
interface UriRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUriRecord(uriRecord: UriRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUriRecords(uriRecords: List<UriRecordEntity>)

    @Upsert
    suspend fun upsertUriRecord(uriRecord: UriRecordEntity)

    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getPagedUriRecords(query: SupportSQLiteQuery): PagingSource<Int, UriRecordEntity>

    // For Total Count
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getTotalUriRecordCount(query: SupportSQLiteQuery): Flow<Int>

    // For Grouping Counts (Generic)
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getGroupCounts(query: SupportSQLiteQuery): Flow<List<GroupCount>>

    // For Grouping Counts by Date specifically
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getDateCounts(query: SupportSQLiteQuery): Flow<List<DateCount>>

    @Query("SELECT * FROM uri_records WHERE uri_record_id = :id")
    suspend fun getUriRecordById(id: Long): UriRecordEntity?

    @Query("DELETE FROM uri_records WHERE uri_record_id = :id")
    suspend fun deleteUriRecordById(id: Long): Int

    // Potentially useful for filters/suggestions
    @Query("SELECT DISTINCT host FROM uri_records ORDER BY host ASC")
    fun getDistinctHosts(): Flow<List<String>>

    // Potentially useful for filters/suggestions (including null)
    @Query("SELECT DISTINCT chosen_browser_package FROM uri_records ORDER BY chosen_browser_package ASC")
    fun getDistinctChosenBrowsers(): Flow<List<String?>>

    @Query("DELETE FROM uri_records")
    suspend fun deleteAllUriRecords()

    @Query("SELECT COUNT(*) FROM uri_records")
    suspend fun count(): Int
}
