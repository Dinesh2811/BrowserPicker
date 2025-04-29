//package browserpicker.data.local.repository
//
//import androidx.paging.PagingConfig
//import androidx.paging.PagingData
//import browserpicker.core.di.InstantProvider
//import browserpicker.core.di.IoDispatcher
//import browserpicker.data.local.datasource.UriHistoryLocalDataSource
//import browserpicker.data.local.query.model.UriRecordQueryConfig
//import browserpicker.domain.model.DomainDateCount
//import browserpicker.domain.model.DomainGroupCount
//import browserpicker.domain.model.InteractionAction
//import browserpicker.domain.model.UriRecord
//import browserpicker.domain.model.UriSource
//import browserpicker.domain.model.query.UriHistoryQuery
//import browserpicker.domain.repository.UriHistoryRepository
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class UriHistoryRepositoryImpl @Inject constructor(
//    private val dataSource: UriHistoryLocalDataSource,
//    private val instantProvider: InstantProvider,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//): UriHistoryRepository {
//    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
//        // NOTE: Advanced filters from the domain layer are not currently supported.
//        // If needed, map them here from the domain query to the data config.
//        return UriRecordQueryConfig(
//            searchQuery = query.searchQuery,
//            filterByUriSource = query.filterByUriSource,
//            filterByInteractionAction = query.filterByInteractionAction,
//            filterByChosenBrowser = query.filterByChosenBrowser,
//            filterByHost = query.filterByHost,
//            filterByDateRange = query.filterByDateRange,
//            sortBy = query.sortBy,
//            sortOrder = query.sortOrder,
//            groupBy = query.groupBy,
//            groupSortOrder = query.groupSortOrder,
//            advancedFilters = emptyList()
//        )
//    }
//
//    override fun getPagedUriRecords(
//        query: UriHistoryQuery,
//        pagingConfig: PagingConfig,
//    ): Flow<PagingData<UriRecord>> {
//        val dataQueryConfig = mapQueryToConfig(query)
//        return dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig)
//            .catch { Timber.e(it, "Error in getPagedUriRecords") }
//            .flowOn(ioDispatcher)
//    }
//
//    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int> {
//        val dataQueryConfig = mapQueryToConfig(query)
//        return dataSource.getTotalUriRecordCount(dataQueryConfig)
//            .catch { Timber.e(it, "Error in getTotalUriRecordCount") }
//            .flowOn(ioDispatcher)
//    }
//
//    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>> {
//        val dataQueryConfig = mapQueryToConfig(query)
//        return dataSource.getGroupCounts(dataQueryConfig).map { list ->
//            list.map { DomainGroupCount(it.groupValue, it.count) }
//        }.catch { Timber.e(it, "Error in getGroupCounts") }
//            .flowOn(ioDispatcher)
//    }
//
//    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>> {
//        val dataQueryConfig = mapQueryToConfig(query)
//        return dataSource.getDateCounts(dataQueryConfig).map { list ->
//            list.map { DomainDateCount(it.date, it.count) }
//        }.catch { Timber.e(it, "Error in getDateCounts") }
//            .flowOn(ioDispatcher)
//    }
//
//
//    override suspend fun addUriRecord(
//        uriString: String,
//        host: String,
//        source: UriSource,
//        action: InteractionAction,
//        chosenBrowser: String?,
//        associatedHostRuleId: Long?,
//    ): Result<Long> = runCatching {
//        withContext(ioDispatcher) {
//            val record = UriRecord(
//                uriString = uriString,
//                host = host,
//                timestamp = instantProvider.now(),
//                uriSource = source,
//                interactionAction = action,
//                chosenBrowserPackage = chosenBrowser,
//                associatedHostRuleId = associatedHostRuleId
//            )
//            dataSource.insertUriRecord(record)
//        }
//    }.onFailure { e ->
//        Timber.e(e, "Failed to add URI record: uriString='$uriString', host='$host', source='$source', action='$action'")
//    }
//
//    override suspend fun getUriRecord(id: Long): UriRecord? = runCatching {
//        withContext(ioDispatcher) {
//            dataSource.getUriRecord(id)
//        }
//    }.getOrElse {
//        Timber.e(it, "Failed to get URI record with id: %d", id)
//        null
//    }
//
//    override suspend fun deleteUriRecord(id: Long): Boolean {
//        return runCatching {
//            withContext(ioDispatcher) {
//                dataSource.deleteUriRecord(id)
//            }
//        }.getOrElse {
//            Timber.e(it, "Failed to delete URI record with id: $id")
//            false
//        }
//    }
//
//    override suspend fun deleteAllUriRecords(): Result<Unit> = runCatching {
//        withContext(ioDispatcher) {
//            dataSource.deleteAllUriRecords()
//        }
//    }.onFailure { Timber.e(it, "Failed to delete all URI records") }
//
//    override fun getDistinctHosts(): Flow<List<String>> {
//        return dataSource.getDistinctHosts()
//            .catch { Timber.e(it, "Error in getDistinctHosts") }
//            .flowOn(ioDispatcher)
//    }
//
//    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
//        return dataSource.getDistinctChosenBrowsers()
//            .catch { Timber.e(it, "Error in getDistinctChosenBrowsers") }
//            .flowOn(ioDispatcher)
//    }
//}
