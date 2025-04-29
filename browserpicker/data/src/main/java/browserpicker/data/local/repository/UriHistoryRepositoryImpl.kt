package browserpicker.data.local.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.data.local.datasource.UriHistoryLocalDataSource
import browserpicker.data.local.query.model.UriRecordQueryConfig
import browserpicker.domain.model.DomainDateCount
import browserpicker.domain.model.DomainGroupCount
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val dataSource: UriHistoryLocalDataSource,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher, // Inject IO dispatcher
): UriHistoryRepository {

    // Mapper function from Domain Query to Data Query Config
    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
        return UriRecordQueryConfig(
            searchQuery = query.searchQuery,
            filterByUriSource = query.filterByUriSource,
            filterByInteractionAction = query.filterByInteractionAction,
            filterByChosenBrowser = query.filterByChosenBrowser,
            filterByHost = query.filterByHost,
            filterByDateRange = query.filterByDateRange,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            groupBy = query.groupBy,
            groupSortOrder = query.groupSortOrder,
            advancedFilters = emptyList() // Advanced filters managed here if needed
        )
    }

    override fun getPagedUriRecords(
        query: UriHistoryQuery,
        pagingConfig: PagingConfig,
    ): Flow<PagingData<UriRecord>> {
        val dataQueryConfig = mapQueryToConfig(query)
        // DataSource now handles Pager creation and mapping
        return dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig)
        // No need for withContext here as Pager handles its own scheduling
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getTotalUriRecordCount(dataQueryConfig)
        // Flow execution context depends on Room's query executor
    }

    // Map Data GroupCount to Domain GroupCount
    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getGroupCounts(dataQueryConfig).map { list ->
            list.map { DomainGroupCount(it.groupValue, it.count) }
        }
        // Flow execution context depends on Room's query executor
    }

    // Map Data DateCount to Domain DateCount
    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getDateCounts(dataQueryConfig).map { list ->
            list.map { DomainDateCount(it.date, it.count) }
        }
        // Flow execution context depends on Room's query executor
    }


    override suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
    ): Result<Long> = runCatching {
        withContext(ioDispatcher) {
            val record = UriRecord(
                uriString = uriString,
                host = host,
                timestamp = instantProvider.now(),
                uriSource = source,
                interactionAction = action,
                chosenBrowserPackage = chosenBrowser,
                associatedHostRuleId = associatedHostRuleId
                // id is auto-generated
            )
            dataSource.insertUriRecord(record)
        }
    }.onFailure { e ->
        Timber.e(e, "Failed to add URI record: uriString='$uriString', host='$host', source='$source', action='$action'")
    }

    override suspend fun getUriRecord(id: Long): UriRecord? {
        // Reading can often skip withContext if DataSource/DAO handles it,
        // but explicit is safer for non-Flow suspend functions.
        return withContext(ioDispatcher) {
            dataSource.getUriRecord(id)
        }
    }

    override suspend fun deleteUriRecord(id: Long): Boolean {
        return runCatching {
            withContext(ioDispatcher) {
                dataSource.deleteUriRecord(id)
            }
        }.getOrElse {
            Timber.e(it, "Failed to delete URI record with id: $id")
            false
        }
    }

    override suspend fun deleteAllUriRecords(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            dataSource.deleteAllUriRecords()
        }
    }.onFailure { Timber.e(it, "Failed to delete all URI records") }


    override fun getDistinctHosts(): Flow<List<String>> {
        return dataSource.getDistinctHosts() // Handled by DataSource/Room
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return dataSource.getDistinctChosenBrowsers() // Handled by DataSource/Room
    }
}
