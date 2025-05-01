package browserpicker.domain.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.UriHistoryQuery
import kotlinx.coroutines.flow.Flow

interface UriHistoryRepository {
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long>
    fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>>
    fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>>

    suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long? = null
    ): MyResult<Long, AppError>

    suspend fun getUriRecord(id: Long): MyResult<UriRecord?, AppError>
    suspend fun deleteUriRecord(id: Long): MyResult<Unit, AppError>
    suspend fun deleteAllUriRecords(): MyResult<Int, AppError>

    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}

/*

interface UriHistoryRepository {
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long>
    fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>>
    fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>>

    suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long? = null
    ): Result<Long>

    suspend fun getUriRecord(id: Long): UriRecord?
    suspend fun deleteUriRecord(id: Long): Result<Unit>
    suspend fun deleteAllUriRecords(): Result<Int>

    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}

 */