package browserpicker.domain.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.MyResult
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.UriHistoryQuery
import kotlinx.coroutines.flow.Flow

interface UriHistoryRepository2 {

    // 1. getPagedUriRecords: Returns Flow<PagingData<UriRecord>>
    // Recommendation: DO NOT wrap with DomainResult. Paging 3 handles state and errors.
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    // Note: You have this function listed twice with the same signature. Assuming it's a typo.

    // 2. getTotalUriRecordCount: Returns Flow<Long>
    // Recommendation: Wrap with DomainResult. Getting a count can fail.
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<DomainResult<Long, AppError>>

    // 3. getGroupCounts: Returns Flow<List<GroupCount>>
    // Recommendation: Wrap with DomainResult. Getting a list can fail, or be an empty success.
    fun getGroupCounts(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>>

    // 4. deleteAllUriRecords: Returns suspend fun Int
    // Recommendation: Wrap with DomainResult. Deleting is an operation that can fail.
    suspend fun deleteAllUriRecords(): DomainResult<Int, AppError>

    // 5. getDistinctHosts: Returns Flow<List<String>>
    // Recommendation: Wrap with DomainResult. Getting a list of strings can fail, or be an empty success.
    fun getDistinctHosts(): Flow<DomainResult<List<String>, AppError>>

    // 6. getDistinctChosenBrowsers: Returns Flow<List<String?>>
    // Recommendation: Wrap with DomainResult. Getting a list can fail, or be an empty success.
    // Note the nullable String in the list is part of the success data type.
    fun getDistinctChosenBrowsers(): Flow<DomainResult<List<String?>, AppError>>
}
interface UriHistoryRepository {
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<DomainResult<Long, AppError>>
    fun getGroupCounts(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>>
    fun getDateCounts(query: UriHistoryQuery): Flow<DomainResult<List<DateCount>, AppError>>

    suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long? = null
    ): DomainResult<Long, AppError>

    suspend fun getUriRecord(id: Long): DomainResult<UriRecord?, AppError>
    suspend fun deleteUriRecord(id: Long): DomainResult<Unit, AppError>
    suspend fun deleteAllUriRecords(): DomainResult<Int, AppError>
    fun getDistinctHosts(): Flow<DomainResult<List<String>, AppError>>
    fun getDistinctChosenBrowsers(): Flow<DomainResult<List<String?>, AppError>>
}

//interface UriHistoryRepository {
//    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
//    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long>
//    fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>>
//    fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>>
//
//    suspend fun addUriRecord(
//        uriString: String,
//        host: String,
//        source: UriSource,
//        action: InteractionAction,
//        chosenBrowser: String?,
//        associatedHostRuleId: Long? = null
//    ): MyResult<Long, AppError>
//
//    suspend fun getUriRecord(id: Long): MyResult<UriRecord?, AppError>
//    suspend fun deleteUriRecord(id: Long): MyResult<Unit, AppError>
//    suspend fun deleteAllUriRecords(): MyResult<Int, AppError>
//
//    fun getDistinctHosts(): Flow<List<String>>
//    fun getDistinctChosenBrowsers(): Flow<List<String?>>
//}

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
    ): Long

    suspend fun getUriRecord(id: Long): UriRecord?
    suspend fun deleteUriRecord(id: Long): Unit
    suspend fun deleteAllUriRecords(): Int

    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}

 */
