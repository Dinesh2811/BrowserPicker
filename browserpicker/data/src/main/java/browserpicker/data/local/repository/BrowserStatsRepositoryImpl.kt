package browserpicker.data.local.repository

import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.data.local.datasource.BrowserStatsLocalDataSource
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.repository.BrowserStatsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserStatsRepositoryImpl @Inject constructor(
    private val dataSource: BrowserStatsLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): BrowserStatsRepository {

    override suspend fun recordBrowserUsage(packageName: String): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
            dataSource.recordBrowserUsage(packageName)
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to record browser usage for: $packageName")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
                else -> AppError.UnknownError("Failed to record browser usage", e)
            }
            MyResult.Error(appError)
        }
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return dataSource.getBrowserStat(packageName)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching browser stat for: %s", packageName)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStats()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats sorted by last used")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteBrowserStat(packageName: String): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
            val deleted = dataSource.deleteBrowserStat(packageName)
            if (deleted) {
                MyResult.Success(Unit)
            } else {
                Timber.w("[Repository] Browser stat for '$packageName' not found or delete failed. Reporting as success.")
                MyResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete browser stat for: $packageName")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
                else -> AppError.UnknownError("Failed to delete browser stat", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun deleteAllStats(): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val count = dataSource.deleteAllStats()
            Timber.d("[Repository] Deleted $count browser stats.")
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete all browser stats")
            MyResult.Error(AppError.UnknownError("Failed to delete all stats", e))
        }
    }
}
