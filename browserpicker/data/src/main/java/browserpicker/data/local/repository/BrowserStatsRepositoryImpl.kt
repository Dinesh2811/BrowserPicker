package browserpicker.data.local.repository

import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.data.local.datasource.BrowserStatsLocalDataSource
import browserpicker.data.local.mapper.BrowserUsageStatMapper
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.repository.BrowserStatsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserStatsRepositoryImpl @Inject constructor(
    private val dataSource: BrowserStatsLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): BrowserStatsRepository {

    override suspend fun recordBrowserUsage(packageName: String): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
            dataSource.recordBrowserUsage(packageName)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to record browser usage for: $packageName")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                else -> AppError.UnknownError("Failed to record browser usage", e)
            }
            DomainResult.Failure(appError)
        }
    }

    override fun getBrowserStat(packageName: String): Flow<DomainResult<BrowserUsageStat?, AppError>> {
        return dataSource.getBrowserStat(packageName)
            .map { entity ->
                entity?.let { statEntity ->
                    runCatching {
                        BrowserUsageStatMapper.toDomainModel(statEntity)
                    }.onFailure {
                        Timber.e(it, "[Repository] Failed to map BrowserUsageStatEntity ${statEntity.browserPackageName} for package $packageName, skipping.")
                    }.getOrNull()
                }.let { DomainResult.Success(it) }
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<DomainResult<List<BrowserUsageStat>, AppError>> {
        return dataSource.getAllBrowserStats()
            .map { entities ->
                entities.mapNotNull { entity ->
                    runCatching {
                        BrowserUsageStatMapper.toDomainModel(entity)
                    }.onFailure {
                        Timber.e(it, "[Repository] Failed to map BrowserUsageStatEntity ${entity.browserPackageName}, skipping in getAllBrowserStats.")
                    }.getOrNull()
                }.let { DomainResult.Success(it) }
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<DomainResult<List<BrowserUsageStat>, AppError>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed()
            .map { entities ->
                entities.mapNotNull { entity ->
                    runCatching {
                        BrowserUsageStatMapper.toDomainModel(entity)
                    }.onFailure {
                        Timber.e(it, "[Repository] Failed to map BrowserUsageStatEntity ${entity.browserPackageName}, skipping in getAllBrowserStatsSortedByLastUsed.")
                    }.getOrNull()
                }.let { DomainResult.Success(it) }
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteBrowserStat(packageName: String): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
            val deleted = dataSource.deleteBrowserStat(packageName)
            if (deleted) {
                DomainResult.Success(Unit)
            } else {
                Timber.w("[Repository] Browser stat for '$packageName' not found or delete failed. Reporting as success.")
                DomainResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete browser stat for: $packageName")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                else -> AppError.UnknownError("Failed to delete browser stat", e)
            }
            DomainResult.Failure(appError)
        }
    }

    override suspend fun deleteAllStats(): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val count = dataSource.deleteAllStats()
            Timber.d("[Repository] Deleted $count browser stats.")
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete all browser stats")
            DomainResult.Failure(AppError.UnknownError("Failed to delete all stats", e))
        }
    }
}
