package browserpicker.domain.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.UriHistoryQuery
import kotlinx.coroutines.flow.Flow

data class DomainGroupCount(val groupValue: String?, val count: Int)
data class DomainDateCount(val dateString: String?, val count: Int)

interface UriHistoryRepository {
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int>
    fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>>
    fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>>

    suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long? = null
    ): Result<Long>

    suspend fun getUriRecord(id: Long): UriRecord?
    suspend fun deleteUriRecord(id: Long): Boolean
    suspend fun deleteAllUriRecords(): Result<Unit>

    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}
