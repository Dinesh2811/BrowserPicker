package browserpicker.data.local.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.core.results.UriValidationError
import browserpicker.data.local.datasource.UriHistoryLocalDataSource
import browserpicker.data.local.query.model.UriRecordQueryConfig
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val dataSource: UriHistoryLocalDataSource,
    private val uriParser: UriParser,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): UriHistoryRepository {
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
            advancedFilters = query.advancedFilters
        )
    }

    override fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig).flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create PagedUriRecords Flow for query: %s", query)
            flowOf(PagingData.empty<UriRecord>()).flowOn(ioDispatcher)
        }
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getTotalUriRecordCount(dataQueryConfig)
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching total URI record count for query: %s", query)
                    emit(0)
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create TotalUriRecordCount Flow for query: %s", query)
            flowOf(0L).flowOn(ioDispatcher)
        }
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getGroupCounts(dataQueryConfig).map { dataGroupCounts ->
                dataGroupCounts.map { GroupCount(it.groupValue, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
            flowOf(emptyList<GroupCount>())
        }
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getDateCounts(dataQueryConfig).map { dataDateCounts ->
                dataDateCounts.map { DateCount(it.date, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
            flowOf(emptyList<DateCount>())
        }
    }

    override suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
    ): MyResult<Long, AppError> = withContext(ioDispatcher) {
        try {
            when {
                uriString.isBlank() -> throw IllegalArgumentException("URI string cannot be blank or empty")
                host.isBlank() -> throw IllegalArgumentException("Host cannot be blank or empty")
                source == UriSource.UNKNOWN -> throw IllegalArgumentException("URI Source cannot be UNKNOWN; use a valid source type")
                action == InteractionAction.UNKNOWN -> throw IllegalArgumentException("Interaction Action cannot be UNKNOWN; use a valid action type")
            }
            if (chosenBrowser != null) {
                if (chosenBrowser.isBlank()) throw IllegalArgumentException("Chosen browser package name cannot be blank if provided.")
            }
            val parsedUriResult = uriParser.parseAndValidateWebUri(uriString)
            if (parsedUriResult.isError) {
                return@withContext MyResult.Error(parsedUriResult.errorOrNull()!!)
            }

            val record = UriRecord(
                uriString = uriString,
                host = host,
                uriSource = source,
                interactionAction = action,
                chosenBrowserPackage = chosenBrowser,
                timestamp = instantProvider.now(),
                associatedHostRuleId = associatedHostRuleId
            )

            val id = dataSource.insertUriRecord(record)
            if (id <= 0) {
                throw IllegalStateException("Failed to insert URI record: received invalid ID $id")
            }

            MyResult.Success(id)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to add URI record: $uriString, host=$host, source=$source, action=$action, browser=$chosenBrowser")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message ?: "Data integrity issue", e)
                is UriValidationError -> e
                else -> AppError.UnknownError("Failed to add URI record", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun getUriRecord(id: Long): MyResult<UriRecord?, AppError> = withContext(ioDispatcher) {
        try {
            val record = dataSource.getUriRecord(id)
            MyResult.Success(record)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
            val appError = when (e) {
                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for record $id", e)
                else -> AppError.UnknownError("Failed to get URI record $id", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun deleteUriRecord(id: Long): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val deleted = dataSource.deleteUriRecord(id)
            if (deleted) {
                MyResult.Success(Unit)
            } else {
                Timber.w("[Repository] URI record with id: $id not found for deletion or delete failed in data source. Reporting as success (item not present).")
                MyResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete URI record with id: %d", id)
            MyResult.Error(AppError.UnknownError("Failed to delete URI record $id", e))
        }
    }

    override suspend fun deleteAllUriRecords(): MyResult<Int, AppError> = withContext(ioDispatcher) {
        try {
            val count = dataSource.deleteAllUriRecords()
            MyResult.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete all URI records")
            MyResult.Error(AppError.UnknownError("Failed to delete all URI records", e))
        }
    }

    override fun getDistinctHosts(): Flow<List<String>> {
        return dataSource.getDistinctHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return dataSource.getDistinctChosenBrowsers()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct chosen browsers")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }
}
