//package browserpicker.data.local.repository
//
//import browserpicker.core.di.IoDispatcher
//import browserpicker.data.local.datasource.BrowserStatsLocalDataSource
//import browserpicker.domain.model.BrowserUsageStat
//import browserpicker.domain.repository.BrowserStatsRepository
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class BrowserStatsRepositoryImpl @Inject constructor(
//    private val dataSource: BrowserStatsLocalDataSource,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//): BrowserStatsRepository {
//
//    override suspend fun recordBrowserUsage(packageName: String): Result<Unit> = runCatching {
//        if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
//        withContext(ioDispatcher) {
//            dataSource.recordBrowserUsage(packageName)
//        }
//    }.onFailure { Timber.e(it, "Failed to record browser usage for: $packageName") }
//
//
//    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
//        return dataSource.getBrowserStat(packageName).flowOn(ioDispatcher)
//    }
//
//    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
//        return dataSource.getAllBrowserStats().flowOn(ioDispatcher)
//    }
//
//    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
//        return dataSource.getAllBrowserStatsSortedByLastUsed().flowOn(ioDispatcher)
//    }
//
//    override suspend fun deleteBrowserStat(packageName: String): Result<Unit> = runCatching {
//        if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
//        withContext(ioDispatcher) {
//            val deleted = dataSource.deleteBrowserStat(packageName)
//            if (!deleted) Timber.w("Browser stat for '$packageName' not found or delete failed.")
//        }
//    }.onFailure { Timber.e(it, "Failed to delete browser stat for: $packageName") }
//
//
//    override suspend fun deleteAllStats(): Result<Unit> = runCatching {
//        withContext(ioDispatcher) {
//            dataSource.deleteAllStats()
//        }
//    }.onFailure { Timber.e(it, "Failed to delete all browser stats") }
//}
