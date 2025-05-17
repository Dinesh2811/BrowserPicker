package browserpicker.domain.usecases.browser

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import kotlinx.coroutines.flow.Flow

interface GetAvailableBrowsersUseCase {
    /**
     * Gets a list of all available browsers installed on the device
     */
    operator fun invoke(): Flow<DomainResult<List<BrowserAppInfo>, AppError>>
}

interface GetPreferredBrowserForHostUseCase {
    /**
     * Gets the preferred browser for a specific host if one is set
     */
    suspend operator fun invoke(host: String): DomainResult<BrowserAppInfo?, AppError>
}

interface SetPreferredBrowserForHostUseCase {
    /**
     * Sets the preferred browser for a specific host
     */
    suspend operator fun invoke(host: String, packageName: String, isEnabled: Boolean = true): DomainResult<Unit, AppError>
}

interface ClearPreferredBrowserForHostUseCase {
    /**
     * Clears the preferred browser setting for a specific host
     */
    suspend operator fun invoke(host: String): DomainResult<Unit, AppError>
}

interface RecordBrowserUsageUseCase {
    /**
     * Records that a browser was used to open a URL
     */
    suspend operator fun invoke(packageName: String): DomainResult<Unit, AppError>
}

interface GetBrowserUsageStatsUseCase {
    /**
     * Gets browser usage statistics, optionally sorted
     */
    operator fun invoke(
        sortBy: BrowserStatSortField = BrowserStatSortField.USAGE_COUNT,
        sortOrder: SortOrder = SortOrder.DESC
    ): Flow<DomainResult<List<BrowserUsageStat>, AppError>>
}

interface GetBrowserUsageStatUseCase {
    /**
     * Gets usage statistics for a specific browser
     */
    operator fun invoke(packageName: String): Flow<DomainResult<BrowserUsageStat?, AppError>>
}

interface GetMostFrequentlyUsedBrowserUseCase {
    /**
     * Gets the most frequently used browser
     */
    operator fun invoke(): Flow<DomainResult<BrowserAppInfo?, AppError>>
}

interface GetMostRecentlyUsedBrowserUseCase {
    /**
     * Gets the most recently used browser
     */
    operator fun invoke(): Flow<DomainResult<BrowserAppInfo?, AppError>>
}
