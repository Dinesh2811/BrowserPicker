package browserpicker.data.local.datasource

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.data.local.dao.UriRecordDao
import browserpicker.data.local.entity.UriRecordEntity
import browserpicker.data.local.query.UriRecordQueryBuilder
import browserpicker.data.local.query.model.UriRecordQueryConfig
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface UriHistoryLocalDataSource {
    fun getPagedUriRecords(config: UriRecordQueryConfig, pagingConfig: PagingConfig): Flow<PagingData<UriRecordEntity>>
    fun getTotalUriRecordCount(config: UriRecordQueryConfig): Flow<Long>
    fun getGroupCounts(config: UriRecordQueryConfig): Flow<List<GroupCount>>
    fun getDateCounts(config: UriRecordQueryConfig): Flow<List<DateCount>>
    suspend fun insertUriRecord(record: UriRecordEntity): Long
    suspend fun insertUriRecords(records: List<UriRecordEntity>)
    suspend fun getUriRecord(id: Long): UriRecordEntity?
    suspend fun deleteUriRecord(id: Long): Boolean
    suspend fun deleteAllUriRecords(): Int
    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}

@Singleton
class UriHistoryLocalDataSourceImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val queryBuilder: UriRecordQueryBuilder,
): UriHistoryLocalDataSource {

    override fun getPagedUriRecords(config: UriRecordQueryConfig, pagingConfig: PagingConfig): Flow<PagingData<UriRecordEntity>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                val query = queryBuilder.buildPagedQuery(config)
                uriRecordDao.getPagedUriRecords(query)
            }
        ).flow
    }

    override fun getTotalUriRecordCount(config: UriRecordQueryConfig): Flow<Long> {
        val query = queryBuilder.buildTotalCountQuery(config)
        return uriRecordDao.getTotalUriRecordCount(query)
    }

    override fun getGroupCounts(config: UriRecordQueryConfig): Flow<List<GroupCount>> {
        val query = queryBuilder.buildGroupCountQuery(config)
        return uriRecordDao.getGroupCounts(query)
    }

    override fun getDateCounts(config: UriRecordQueryConfig): Flow<List<DateCount>> {
        val query = queryBuilder.buildDateCountQuery(config)
        return uriRecordDao.getDateCounts(query)
    }

    override suspend fun insertUriRecord(record: UriRecordEntity): Long {
        return uriRecordDao.insertUriRecord(record)
    }

    override suspend fun insertUriRecords(records: List<UriRecordEntity>) {
        uriRecordDao.insertUriRecords(records)
    }

    override suspend fun getUriRecord(id: Long): UriRecordEntity? {
        return uriRecordDao.getUriRecordById(id)
    }

    override suspend fun deleteUriRecord(id: Long): Boolean {
        return uriRecordDao.deleteUriRecordById(id) > 0
    }

    override suspend fun deleteAllUriRecords(): Int {
        return uriRecordDao.deleteAllUriRecords()
    }

    override fun getDistinctHosts(): Flow<List<String>> {
        return uriRecordDao.getDistinctHosts()
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return uriRecordDao.getDistinctChosenBrowsers()
    }
}
