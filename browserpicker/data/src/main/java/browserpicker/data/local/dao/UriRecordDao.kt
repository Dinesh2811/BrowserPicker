package browserpicker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.data.local.entity.UriRecordEntity
import browserpicker.domain.model.InteractionAction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface UriRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: UriRecordEntity): Long

    @Query("SELECT * FROM uri_records WHERE uri_record_id = :id")
    suspend fun getRecordById(id: Long): UriRecordEntity?

    // --- Pagination for History ---
    // Base query used by PagingSource. We'll append ORDER BY dynamically.
    // Note: Room can automatically handle simple query construction for PagingSource,
    // but for complex filtering/sorting, RawQuery might be needed later.
    // This example uses simple LIKE for basic filtering.
    @Query("""
        SELECT * FROM uri_records
        WHERE (:searchTerm IS NULL OR uri_string LIKE '%' || :searchTerm || '%')
        ORDER BY
            CASE WHEN :isSortAsc = 1 THEN timestamp END ASC,
            CASE WHEN :isSortAsc = 0 THEN timestamp END DESC
    """)
    fun getHistoryPagingSource(
        searchTerm: String?,
        isSortAsc: Boolean
    ): PagingSource<Int, UriRecordEntity>

    // Example using RawQuery for more complex dynamic sorting/filtering if needed later
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getHistoryPagingSourceRaw(query: SupportSQLiteQuery): PagingSource<Int, UriRecordEntity>

    // --- Specific Queries ---
    @Query("SELECT * FROM uri_records WHERE host_rule_id = :hostRuleId ORDER BY timestamp DESC")
    fun observeRecordsByHostRuleId(hostRuleId: Long): Flow<List<UriRecordEntity>>

    @Query("SELECT COUNT(*) FROM uri_records")
    suspend fun getHistoryCount(): Long

    // --- Deletion ---
    @Query("DELETE FROM uri_records WHERE uri_record_id = :id")
    suspend fun deleteById(id: Long): Int // Returns number of rows affected

    @Query("DELETE FROM uri_records")
    suspend fun clearAllHistory(): Int // Returns number of rows affected

    // Potentially useful for analysis - Get counts grouped by certain criteria
    @Query("""
        SELECT interaction_action, COUNT(*) as count
        FROM uri_records
        WHERE timestamp >= :since
        GROUP BY interaction_action
    """)
    suspend fun getInteractionCountsSince(since: Instant): List<InteractionCountTuple>

    // You might need a simple data class for projection results
    data class InteractionCountTuple(
        @androidx.room.ColumnInfo(name = "interaction_action") val action: InteractionAction,
        @androidx.room.ColumnInfo(name = "count") val count: Int
    )
}
