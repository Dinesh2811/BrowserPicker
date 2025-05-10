package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.*
import browserpicker.domain.model.* // For BrowserUsageStat, DateCount, etc.
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.UriHistoryRepository // Potentially for daily usage breakdown
import browserpicker.domain.usecases.analytics.BrowserUsageReport
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

class GenerateBrowserUsageReportUseCaseImpl @Inject constructor(
    private val browserStatsRepository: BrowserStatsRepository,
    private val uriHistoryRepository: UriHistoryRepository, // For detailed daily breakdown if needed
    private val clock: Clock
) : GenerateBrowserUsageReportUseCase {

    override suspend operator fun invoke(
        timeRange: Pair<Instant, Instant>?,
        exportToFile: Boolean, // Hint for data comprehensiveness
        filePath: String?      // Hint for data comprehensiveness
    ): DomainResult<BrowserUsageReport, AppError> {
        return try {
            coroutineScope {
                val actualTimeRange = timeRange ?: Pair(Instant.DISTANT_PAST, clock.now())

                val allStatsResultDeferred = async { browserStatsRepository.getAllBrowserStats().first() }

                val allStatsResult = allStatsResultDeferred.await()
                if (allStatsResult.isFailure) {
                    return@coroutineScope DomainResult.Failure(allStatsResult.errorOrNull()!!)
                }
                val allStats = allStatsResult.getOrNull() ?: emptyList()

                val totalUsage = allStats.associate { it.browserPackageName to it.usageCount }
                val mostUsed = allStats.maxByOrNull { it.usageCount }
                val mostRecent = allStats.maxByOrNull { it.lastUsedTimestamp }

                // For dailyUsage: Map<String, List<DateCount>> (browser package to its daily trend)
                // This is complex. BrowserStatsRepository doesn't provide this directly.
                // We might need to derive it from UriHistoryRepository by querying UriRecords,
                // filtering by chosenBrowserPackage and date, then grouping.
                // This is a significant operation.

                // Placeholder for dailyUsage - requires more involved querying from UriHistoryRepository
                val dailyUsageData = mutableMapOf<String, List<DateCount>>()
                // Conceptual: Iterate through known browsers from allStats
                // For each browser, query UriHistoryRepository for records within actualTimeRange
                // and group by date. This can be very intensive.
                /*
                allStats.forEach { browserStat ->
                    val browserDailyQuery = UriHistoryQuery.DEFAULT.copy(
                        filterByChosenBrowser = setOf(browserStat.browserPackageName),
                        filterByDateRange = actualTimeRange
                    )
                    val dailyCountsResult = uriHistoryRepository.getDateCounts(browserDailyQuery).first()
                    if (dailyCountsResult.isSuccess) {
                        dailyUsageData[browserStat.browserPackageName] = dailyCountsResult.getOrNull() ?: emptyList()
                    }
                    // Handle error accumulation if needed
                }
                */
                // For now, keeping dailyUsage empty or simplified as it's non-trivial

                DomainResult.Success(
                    BrowserUsageReport(
                        totalUsage = totalUsage,
                        timeRange = actualTimeRange,
                        mostUsed = mostUsed,
                        mostRecent = mostRecent,
                        dailyUsage = dailyUsageData // Placeholder: empty
                    )
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DomainResult.Failure(AppError.UnknownError("Failed to generate browser usage report", e))
        }
    }
} 