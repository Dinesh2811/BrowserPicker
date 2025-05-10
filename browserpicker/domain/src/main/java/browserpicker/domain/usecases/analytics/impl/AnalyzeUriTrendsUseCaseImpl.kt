package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class AnalyzeUriTrendsUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository
) : AnalyzeUriTrendsUseCase {

    override operator fun invoke(
        timeRange: Pair<Instant, Instant>?
    ): Flow<DomainResult<Map<String, List<DateCount>>, AppError>> = flow {
        // Base query
        var query = UriHistoryQuery.DEFAULT

        // Apply time range if provided
        if (timeRange != null) {
            query = query.copy(filterByDateRange = timeRange)
        }

        // For this use case, we need to analyze trends. This typically means
        // fetching relevant UriRecords and then processing them.
        // The UriHistoryRepository currently provides getGroupCounts and getDateCounts.
        // getDateCounts provides overall counts by date.
        // getGroupCounts provides counts by a specific group (host, action, etc.).

        // To get trends *per host* (or other dimension) over time, we might need:
        // 1. Fetch all records in the time range.
        // 2. Group them by host (or other dimension) client-side.
        // 3. Then, for each group, further group by date and count.
        // OR
        // 4. A more specialized repository method.

        // For now, let's demonstrate using getDateCounts for overall trends as a placeholder.
        // A more complete implementation would require more complex data aggregation logic
        // to return Map<String, List<DateCount>> where the String key is a dimension like "host".

        // Placeholder: Using getDateCounts which returns List<DateCount> for the overall dataset.
        // This doesn't directly match Map<String, List<DateCount>> unless we map it to a single key.
        uriHistoryRepository.getDateCounts(query)
            .map { result ->
                result.mapSuccess { dateCounts ->
                    // Example: Group all date counts under a generic key "overall_trend"
                    // A real implementation would group by specific criteria (e.g., host)
                    mapOf("overall_trend" to dateCounts)
                }
            }
            .collect { emit(it) }

    }.catchUnexpected()
} 