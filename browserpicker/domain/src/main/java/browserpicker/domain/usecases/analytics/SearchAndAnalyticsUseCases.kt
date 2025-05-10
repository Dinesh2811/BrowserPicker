package browserpicker.domain.usecases.analytics

import androidx.compose.runtime.Immutable
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.*
import browserpicker.domain.service.PagingDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface AnalyzeUriTrendsUseCase {
    /**
     * Analyzes URI trends over time (most visited hosts, interactions, etc.)
     */
    operator fun invoke(
        timeRange: Pair<Instant, Instant>? = null
    ): Flow<DomainResult<Map<String, List<DateCount>>, AppError>>
}

interface AnalyzeBrowserUsageTrendsUseCase {
    /**
     * Analyzes browser usage trends over time
     */
    operator fun invoke(
        timeRange: Pair<Instant, Instant>? = null
    ): Flow<DomainResult<Map<String, List<DateCount>>, AppError>>
}

interface GetMostVisitedHostsUseCase {
    /**
     * Gets the most visited hosts
     */
    operator fun invoke(
        limit: Int = 10,
        timeRange: Pair<Instant, Instant>? = null
    ): Flow<DomainResult<List<GroupCount>, AppError>>
}

interface GetTopActionsByHostUseCase {
    /**
     * Gets the most common actions for a specific host
     */
    operator fun invoke(
        host: String
    ): Flow<DomainResult<List<GroupCount>, AppError>>
}

interface SearchHostRulesUseCase {
    /**
     * Searches for host rules matching the query string
     */
    operator fun invoke(
        query: String,
        filterByStatus: UriStatus? = null,
        includeFolderId: Long? = null,
        pagingConfig: PagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG
    ): Flow<PagingData<HostRule>>
}

interface SearchFoldersUseCase {
    /**
     * Searches for folders matching the query string
     */
    operator fun invoke(
        query: String,
        folderType: FolderType? = null
    ): Flow<DomainResult<List<Folder>, AppError>>
}

interface GenerateHistoryReportUseCase {
    /**
     * Generates a comprehensive report of URI history activity
     */
    suspend operator fun invoke(
        timeRange: Pair<Instant, Instant>? = null,
        exportToFile: Boolean = false,
        filePath: String? = null
    ): DomainResult<UriHistoryReport, AppError>
}

interface GenerateBrowserUsageReportUseCase {
    /**
     * Generates a comprehensive report of browser usage
     */
    suspend operator fun invoke(
        timeRange: Pair<Instant, Instant>? = null,
        exportToFile: Boolean = false,
        filePath: String? = null
    ): DomainResult<BrowserUsageReport, AppError>
}

interface TrackUriActionUseCase {
    /**
     * Tracks actions performed on a URI, such as bookmarking, blocking, or preference changes.
     */
    suspend operator fun invoke(uriString: String, action: InteractionAction, associatedHostRuleId: Long? = null): DomainResult<Unit, AppError>
}

interface AnalyzeUriStatusChangesUseCase {
    /**
     * Analyzes trends in URI status changes (e.g., from bookmarked to blocked) over a specified time range.
     */
    operator fun invoke(timeRange: Pair<Instant, Instant>? = null): Flow<DomainResult<Map<String, List<DateCount>>, AppError>>
}

@Immutable
data class UriHistoryReport(
    val totalRecords: Long,
    val timeRange: Pair<Instant, Instant>,
    val topHosts: List<GroupCount>,
    val actionBreakdown: Map<InteractionAction, Long>,
    val sourceBreakdown: Map<UriSource, Long>,
    val browserBreakdown: Map<String?, Long>,
    val dailyActivity: List<DateCount>
)

@Immutable
data class BrowserUsageReport(
    val totalUsage: Map<String, Long>,
    val timeRange: Pair<Instant, Instant>,
    val mostUsed: BrowserUsageStat?,
    val mostRecent: BrowserUsageStat?,
    val dailyUsage: Map<String, List<DateCount>>
) 