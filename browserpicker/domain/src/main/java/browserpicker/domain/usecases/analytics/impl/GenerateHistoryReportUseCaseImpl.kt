package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.analytics.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.analytics.UriHistoryReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

class GenerateHistoryReportUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val clock: Clock
) : GenerateHistoryReportUseCase {

    override suspend operator fun invoke(
        timeRange: Pair<Instant, Instant>?,
        exportToFile: Boolean, // Domain layer should not handle file I/O. This is a hint for data comprehensiveness.
        filePath: String?      // Same as above.
    ): DomainResult<UriHistoryReport, AppError> {
        return try {
            coroutineScope { // Scope for concurrent execution of repository calls
                val actualTimeRange = timeRange ?: Pair(Instant.DISTANT_PAST, clock.now())

                val totalRecordsQuery = UriHistoryQuery.DEFAULT.copy(filterByDateRange = actualTimeRange)
                val topHostsQuery = UriHistoryQuery.DEFAULT.copy(filterByDateRange = actualTimeRange, groupBy = UriRecordGroupField.HOST)
                val actionBreakdownQuery = UriHistoryQuery.DEFAULT.copy(filterByDateRange = actualTimeRange, groupBy = UriRecordGroupField.INTERACTION_ACTION)
                val sourceBreakdownQuery = UriHistoryQuery.DEFAULT.copy(filterByDateRange = actualTimeRange, groupBy = UriRecordGroupField.URI_SOURCE)
                val browserBreakdownQuery = UriHistoryQuery.DEFAULT.copy(filterByDateRange = actualTimeRange, groupBy = UriRecordGroupField.CHOSEN_BROWSER)
                val dailyActivityQuery = UriHistoryQuery.DEFAULT.copy(filterByDateRange = actualTimeRange)

                // Launch queries concurrently
                val totalRecordsDeferred = async { uriHistoryRepository.getTotalUriRecordCount(totalRecordsQuery).first() }
                val topHostsDeferred = async { uriHistoryRepository.getGroupCounts(topHostsQuery).first() }
                val actionBreakdownDeferred = async { uriHistoryRepository.getGroupCounts(actionBreakdownQuery).first() }
                val sourceBreakdownDeferred = async { uriHistoryRepository.getGroupCounts(sourceBreakdownQuery).first() }
                val browserBreakdownDeferred = async { uriHistoryRepository.getGroupCounts(browserBreakdownQuery).first() }
                val dailyActivityDeferred = async { uriHistoryRepository.getDateCounts(dailyActivityQuery).first() }

                // Await results and handle potential failures
                val totalRecordsResult = totalRecordsDeferred.await()
                val topHostsResult = topHostsDeferred.await()
                val actionBreakdownResult = actionBreakdownDeferred.await()
                val sourceBreakdownResult = sourceBreakdownDeferred.await()
                val browserBreakdownResult = browserBreakdownDeferred.await()
                val dailyActivityResult = dailyActivityDeferred.await()

                // Check each result and propagate first failure
                listOf(
                    totalRecordsResult, topHostsResult, actionBreakdownResult,
                    sourceBreakdownResult, browserBreakdownResult, dailyActivityResult
                ).firstNotNullOfOrNull { it.errorOrNull() }?.let {
                    return@coroutineScope DomainResult.Failure(it)
                }

                // All results are successful, extract data
                val totalRecords = totalRecordsResult.getOrNull() ?: 0L
                val topHosts = topHostsResult.getOrNull()?.take(10) ?: emptyList() // Example: take top 10 hosts
                val actionBreakdown = actionBreakdownResult.getOrNull()
                    ?.associate { InteractionAction.fromValue(it.groupValue?.toIntOrNull() ?: -1) to it.count.toLong() }
                    ?.filterKeys { it != InteractionAction.UNKNOWN } ?: emptyMap()
                val sourceBreakdown = sourceBreakdownResult.getOrNull()
                    ?.associate { UriSource.fromValue(it.groupValue?.toIntOrNull() ?: -1) to it.count.toLong() }
                    ?.filterKeys { it != UriSource.UNKNOWN } ?: emptyMap()
                val browserBreakdown = browserBreakdownResult.getOrNull()
                    ?.associate { it.groupValue to it.count.toLong() } ?: emptyMap()
                val dailyActivity = dailyActivityResult.getOrNull() ?: emptyList()

                DomainResult.Success(
                    UriHistoryReport(
                        totalRecords = totalRecords,
                        timeRange = actualTimeRange,
                        topHosts = topHosts,
                        actionBreakdown = actionBreakdown,
                        sourceBreakdown = sourceBreakdown,
                        browserBreakdown = browserBreakdown,
                        dailyActivity = dailyActivity
                    )
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DomainResult.Failure(AppError.UnknownError("Failed to generate history report", e))
        }
    }
} 