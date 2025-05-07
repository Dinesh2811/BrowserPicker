//package browserpicker.data.local.repository
//
//import androidx.paging.PagingConfig
//import androidx.paging.PagingData
//import androidx.paging.map
//import browserpicker.core.di.InstantProvider
//import browserpicker.core.di.IoDispatcher
//import browserpicker.core.results.AppError
//import browserpicker.core.results.DomainResult
//import browserpicker.data.DataNotFoundException
//import browserpicker.data.local.datasource.UriHistoryLocalDataSource
//import browserpicker.data.local.mapper.UriRecordMapper
//import browserpicker.data.local.query.model.UriRecordQueryConfig
//import browserpicker.domain.model.DateCount
//import browserpicker.domain.model.GroupCount
//import browserpicker.domain.model.InteractionAction
//import browserpicker.domain.model.UriRecord
//import browserpicker.domain.model.UriSource
//import browserpicker.domain.model.query.UriHistoryQuery
//import browserpicker.domain.repository.UriHistoryRepository
//import browserpicker.domain.service.UriParser
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import javax.inject.*
//
//@Singleton
//class UriHistoryRepositoryImpl @Inject constructor(
//    private val dataSource: UriHistoryLocalDataSource,
//    private val uriParser: UriParser,
//    private val instantProvider: InstantProvider,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//): UriHistoryRepository {
//    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
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
//            advancedFilters = query.advancedFilters
//        )
//    }
//
//    override fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>> {
//        return try {
//            val dataQueryConfig = mapQueryToConfig(query)
//            dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig)
//                .map { pagingDataEntity ->
//                    pagingDataEntity.map { entity ->
//                        UriRecordMapper.toDomainModel(entity)
//                    }
//                }
//                .flowOn(ioDispatcher)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to create PagedUriRecords Flow for query: %s", query)
//            flowOf(PagingData.empty<UriRecord>()).flowOn(ioDispatcher)
//        }
//    }
//
//    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<DomainResult<Long, AppError>> {
//        return try {
//            val dataQueryConfig = mapQueryToConfig(query)
//            dataSource.getTotalUriRecordCount(dataQueryConfig)
//                .map { count ->
//                    DomainResult.Success(count) as DomainResult<Long, AppError>
//                }
//                .catch { e ->
//                    Timber.e(e, "[Repository] Error fetching total URI record count for query: %s", query)
//                    emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching total count", e)))
//                }
//                .flowOn(ioDispatcher)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to create TotalUriRecordCount Flow for query: %s", query)
//            flowOf(DomainResult.Failure(AppError.UnknownError("Failed to create TotalUriRecordCount Flow", e))).flowOn(ioDispatcher)
//        }
//    }
//
//    override fun getGroupCounts(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>> {
//        return try {
//            val dataQueryConfig = mapQueryToConfig(query)
//            dataSource.getGroupCounts(dataQueryConfig)
//                .map { counts ->
//                    DomainResult.Success(counts) as DomainResult<List<GroupCount>, AppError>
//                }
//                .catch { e ->
//                    Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
//                    emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching group counts", e)))
//                }
//                .flowOn(ioDispatcher)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
//            flowOf(DomainResult.Failure(AppError.UnknownError("Failed to create GroupCounts Flow", e))).flowOn(ioDispatcher)
//        }
//    }
//
//    override fun getDateCounts(query: UriHistoryQuery): Flow<DomainResult<List<DateCount>, AppError>> {
//        return try {
//            val dataQueryConfig = mapQueryToConfig(query)
//            dataSource.getDateCounts(dataQueryConfig)
//                .map { counts ->
//                    DomainResult.Success(counts) as DomainResult<List<DateCount>, AppError>
//                }
//                .catch { e ->
//                    Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
//                    emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching date counts", e)))
//                }
//                .flowOn(ioDispatcher)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
//            flowOf(DomainResult.Failure(AppError.UnknownError("Failed to create DateCounts Flow", e))).flowOn(ioDispatcher)
//        }
//    }
//    private fun validateAddUriRecordInput(
//        uriString: String,
//        host: String,
//        source: UriSource,
//        action: InteractionAction,
//        chosenBrowser: String?
//    ): AppError.ValidationError? {
//        return when {
//            uriString.isBlank() -> AppError.ValidationError("URI string cannot be blank or empty")
//            host.isBlank() -> AppError.ValidationError("Host cannot be blank or empty")
//            source == UriSource.UNKNOWN -> AppError.ValidationError("URI Source cannot be UNKNOWN; use a valid source type")
//            action == InteractionAction.UNKNOWN -> AppError.ValidationError("Interaction Action cannot be UNKNOWN; use a valid action type")
//            chosenBrowser != null && chosenBrowser.isBlank() -> AppError.ValidationError("Chosen browser package name cannot be blank if provided.")
//            else -> null
//        }
//    }
//
//    override suspend fun addUriRecord(
//        uriString: String,
//        host: String,
//        source: UriSource,
//        action: InteractionAction,
//        chosenBrowser: String?,
//        associatedHostRuleId: Long?,
//    ): DomainResult<Long, AppError> = withContext(ioDispatcher) {
//        try {
////            when {
////                uriString.isBlank() -> return@withContext DomainResult.Failure(AppError.ValidationError("URI string cannot be blank or empty"))
////                host.isBlank() -> return@withContext DomainResult.Failure(AppError.ValidationError("Host cannot be blank or empty"))
////                source == UriSource.UNKNOWN -> return@withContext DomainResult.Failure(AppError.ValidationError("URI Source cannot be UNKNOWN; use a valid source type"))
////                action == InteractionAction.UNKNOWN -> return@withContext DomainResult.Failure(AppError.ValidationError("Interaction Action cannot be UNKNOWN; use a valid action type"))
////            }
////            if (chosenBrowser != null) {
////                if (chosenBrowser.isBlank()) return@withContext DomainResult.Failure(AppError.ValidationError("Chosen browser package name cannot be blank if provided."))
////            }
//
//            val validationError = validateAddUriRecordInput(uriString, host, source, action, chosenBrowser)
//            if (validationError != null) {
//                Timber.e("[Repository] Failed to add URI record: ${validationError.message}")
//                return@withContext DomainResult.Failure(validationError)
//            }
//
//            val parsedUriResult = uriParser.parseAndValidateWebUri(uriString)
//            if (parsedUriResult is DomainResult.Failure) {
//                Timber.e(parsedUriResult.error.cause, "[Repository] URI parsing and validation failed for $uriString: ${parsedUriResult.error.message}")
//                return@withContext DomainResult.Failure(parsedUriResult.error)
//            }
//
//            val record = UriRecord(
//                uriString = uriString,
//                host = host,
//                uriSource = source,
//                interactionAction = action,
//                chosenBrowserPackage = chosenBrowser,
//                timestamp = instantProvider.now(),
//                associatedHostRuleId = associatedHostRuleId
//            )
//
//            val entity = UriRecordMapper.toEntity(record)
//            val id = dataSource.insertUriRecord(entity)
//            if (id <= 0) {
//                Timber.e("[Repository] Failed to insert URI record: received invalid ID $id for $uriString")
//                return@withContext DomainResult.Failure(AppError.DataIntegrityError("Failed to insert URI record: received invalid ID $id"))
//            }
//
//            DomainResult.Success(id)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] An unexpected error occurred during URI record addition process for $uriString")
//            DomainResult.Failure(AppError.UnknownError("An unexpected error occurred while adding URI record.", e))
//        }
//    }
//
//    override suspend fun getUriRecord(id: Long): DomainResult<UriRecord?, AppError> = withContext(ioDispatcher) {
//        try {
//            val entity = dataSource.getUriRecord(id)
//            val record = entity?.let { UriRecordMapper.toDomainModel(it) }
//            DomainResult.Success(record)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
//            val appError = when (e) {
//                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for record $id", e)
//                is DataNotFoundException -> AppError.DataNotFound("URI record with id $id not found", e)
//                else -> AppError.UnknownError("An unexpected error occurred while getting URI record $id.", e)
//            }
//            DomainResult.Failure(appError)
//        }
//    }
//
//    override suspend fun deleteUriRecord(id: Long): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
//        try {
//            val deleted = dataSource.deleteUriRecord(id)
//            if (deleted > 0) {
//                DomainResult.Success(Unit)
//            } else {
//                Timber.w("[Repository] URI record with id: $id not found for deletion or delete failed in data source. Reporting as success (item not present).")
//                DomainResult.Success(Unit)
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to delete URI record with id: %d", id)
//            val appError = AppError.DatabaseError(e.message ?: "Database error deleting URI record $id", e)
//            DomainResult.Failure(appError)
//        }
//    }
//
//    override suspend fun deleteAllUriRecords(): DomainResult<Int, AppError> = withContext(ioDispatcher) {
//        try {
//            val count = dataSource.deleteAllUriRecords()
//            DomainResult.Success(count)
//        } catch (e: Exception) {
//            Timber.e(e, "[Repository] Failed to delete all URI records")
//            val appError = AppError.DatabaseError(e.message ?: "Database error deleting all URI records", e)
//            DomainResult.Failure(appError)
//        }
//    }
//
//    override fun getDistinctHosts(): Flow<DomainResult<List<String>, AppError>> {
//        return dataSource.getDistinctHosts()
//            .map { hosts ->
//                DomainResult.Success(hosts) as DomainResult<List<String>, AppError>
//            }
//            .catch { e ->
//                Timber.e(e, "[Repository] Error fetching distinct hosts")
//                emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching distinct hosts", e)))
//            }
//            .flowOn(ioDispatcher)
//    }
//
//    override fun getDistinctChosenBrowsers(): Flow<DomainResult<List<String?>, AppError>> {
//        return dataSource.getDistinctChosenBrowsers()
//            .map { browsers ->
//                DomainResult.Success(browsers) as DomainResult<List<String?>, AppError>
//            }
//            .catch { e ->
//                Timber.e(e, "[Repository] Error fetching distinct chosen browsers")
//                emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching distinct chosen browsers", e)))
//            }
//            .flowOn(ioDispatcher)
//    }
//}
