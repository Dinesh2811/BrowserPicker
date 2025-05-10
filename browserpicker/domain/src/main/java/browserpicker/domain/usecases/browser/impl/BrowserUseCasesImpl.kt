package browserpicker.domain.usecases.browser.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.usecases.browser.*
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAvailableBrowsersUseCaseImpl @Inject constructor(
    // This would typically need a system service repository to get installed browsers
//    private val systemServiceRepository: Any // Placeholder, replace with actual repository
) : GetAvailableBrowsersUseCase {
    override fun invoke(): Flow<DomainResult<List<BrowserAppInfo>, AppError>> {
        // This is a simplified implementation
        // In a real implementation, you would query the system for installed browsers
        return kotlinx.coroutines.flow.flowOf(
            DomainResult.Success(emptyList())
        )
    }
}

class GetPreferredBrowserForHostUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository
) : GetPreferredBrowserForHostUseCase {
    override fun invoke(host: String): Flow<DomainResult<BrowserAppInfo?, AppError>> {
        if (host.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(
                DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
            )
        }

        return hostRuleRepository.getHostRuleByHost(host).map { result ->
            when (result) {
                is DomainResult.Success -> {
                    val hostRule = result.data
                    if (hostRule != null &&
                        hostRule.uriStatus == UriStatus.BOOKMARKED &&
                        hostRule.preferredBrowserPackage != null &&
                        hostRule.isPreferenceEnabled) {

                        // In a real implementation, we would query for app name based on package name
                        // For simplicity, we're using the package name as the app name here
                        val appName = hostRule.preferredBrowserPackage.substringAfterLast('.', hostRule.preferredBrowserPackage)

                        DomainResult.Success(
                            BrowserAppInfo(
                                appName = appName,
                                packageName = hostRule.preferredBrowserPackage
                            )
                        )
                    } else {
                        DomainResult.Success(null)
                    }
                }
                is DomainResult.Failure -> DomainResult.Failure(result.error)
            }
        }
    }
}

class SetPreferredBrowserForHostUseCaseImpl @Inject constructor(
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val hostRuleRepository: HostRuleRepository
) : SetPreferredBrowserForHostUseCase {
    override suspend fun invoke(host: String, packageName: String, isEnabled: Boolean): DomainResult<Unit, AppError> {
        if (host.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
        }

        if (packageName.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Package name cannot be blank"))
        }

        // Check if there's an existing rule for this host
        val hostRuleResult = hostRuleRepository.getHostRuleByHost(host).first()
        if (hostRuleResult is DomainResult.Failure) {
            return DomainResult.Failure(hostRuleResult.error)
        }

        val hostRule = (hostRuleResult as DomainResult.Success).data

        // If the host is blocked, we cannot set a preferred browser
        if (hostRule != null && hostRule.uriStatus == UriStatus.BLOCKED) {
            return DomainResult.Failure(
                AppError.ValidationError("Cannot set preferred browser for a blocked host")
            )
        }

        // If there's an existing rule, update it with the new browser preference
        val folderId = hostRule?.folderId
        val status = hostRule?.uriStatus ?: UriStatus.BOOKMARKED // Default to BOOKMARKED if no existing rule

        val saveResult = saveHostRuleUseCase(
            host = host,
            status = status,
            folderId = folderId,
            preferredBrowserPackage = packageName,
            isPreferenceEnabled = isEnabled
        )

        return when (saveResult) {
            is DomainResult.Success -> DomainResult.Success(Unit)
            is DomainResult.Failure -> DomainResult.Failure(saveResult.error)
        }
    }
}

class ClearPreferredBrowserForHostUseCaseImpl @Inject constructor(
    private val hostRuleRepository: HostRuleRepository,
    private val saveHostRuleUseCase: SaveHostRuleUseCase
) : ClearPreferredBrowserForHostUseCase {
    override suspend fun invoke(host: String): DomainResult<Unit, AppError> {
        if (host.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Host cannot be blank"))
        }

        // Check if there's an existing rule for this host
        val hostRuleResult = hostRuleRepository.getHostRuleByHost(host).first()
        if (hostRuleResult is DomainResult.Failure) {
            return DomainResult.Failure(hostRuleResult.error)
        }

        val hostRule = (hostRuleResult as DomainResult.Success).data
            ?: return DomainResult.Success(Unit) // No rule exists, nothing to clear

        // If the host has a status other than NONE, update it to keep the status but remove the browser preference
        if (hostRule.uriStatus != UriStatus.NONE) {
            val saveResult = saveHostRuleUseCase(
                host = host,
                status = hostRule.uriStatus,
                folderId = hostRule.folderId,
                preferredBrowserPackage = null,
                isPreferenceEnabled = false
            )

            return when (saveResult) {
                is DomainResult.Success -> DomainResult.Success(Unit)
                is DomainResult.Failure -> DomainResult.Failure(saveResult.error)
            }
        }

        // If status is NONE, we can just delete the rule
        return hostRuleRepository.deleteHostRuleByHost(host)
    }
}

class RecordBrowserUsageUseCaseImpl @Inject constructor(
    private val browserStatsRepository: BrowserStatsRepository
) : RecordBrowserUsageUseCase {
    override suspend fun invoke(packageName: String): DomainResult<Unit, AppError> {
        if (packageName.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Package name cannot be blank"))
        }

        return browserStatsRepository.recordBrowserUsage(packageName)
    }
}

class GetBrowserUsageStatsUseCaseImpl @Inject constructor(
    private val browserStatsRepository: BrowserStatsRepository
) : GetBrowserUsageStatsUseCase {
    override fun invoke(
        sortBy: BrowserStatSortField,
        sortOrder: SortOrder
    ): Flow<DomainResult<List<BrowserUsageStat>, AppError>> {
        return when (sortBy) {
            BrowserStatSortField.USAGE_COUNT -> {
                browserStatsRepository.getAllBrowserStats().map { result ->
                    result.mapSuccess { stats ->
                        when (sortOrder) {
                            SortOrder.ASC -> stats.sortedBy { it.usageCount }
                            SortOrder.DESC -> stats.sortedByDescending { it.usageCount }
                        }
                    }
                }
            }
            BrowserStatSortField.LAST_USED_TIMESTAMP -> {
                // The repository already has a method for this sorting
                if (sortOrder == SortOrder.DESC) {
                    browserStatsRepository.getAllBrowserStatsSortedByLastUsed()
                } else {
                    // For ascending order, we need to reverse the list
                    browserStatsRepository.getAllBrowserStatsSortedByLastUsed().map { result ->
                        result.mapSuccess { stats -> stats.reversed() }
                    }
                }
            }
        }
    }
}

class GetBrowserUsageStatUseCaseImpl @Inject constructor(
    private val browserStatsRepository: BrowserStatsRepository
) : GetBrowserUsageStatUseCase {
    override fun invoke(packageName: String): Flow<DomainResult<BrowserUsageStat?, AppError>> {
        if (packageName.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(
                DomainResult.Failure(AppError.ValidationError("Package name cannot be blank"))
            )
        }

        return browserStatsRepository.getBrowserStat(packageName)
    }
}

class GetMostFrequentlyUsedBrowserUseCaseImpl @Inject constructor(
    private val browserStatsRepository: BrowserStatsRepository
) : GetMostFrequentlyUsedBrowserUseCase {
    override fun invoke(): Flow<DomainResult<BrowserAppInfo?, AppError>> {
        return browserStatsRepository.getAllBrowserStats().map { result ->
            when (result) {
                is DomainResult.Success -> {
                    val stats = result.data
                    if (stats.isEmpty()) {
                        DomainResult.Success(null)
                    } else {
                        val mostFrequent = stats.maxByOrNull { it.usageCount }
                        if (mostFrequent != null) {
                            // In a real implementation, we would query for app name based on package name
                            val appName = mostFrequent.browserPackageName.substringAfterLast('.', mostFrequent.browserPackageName)

                            DomainResult.Success(
                                BrowserAppInfo(
                                    appName = appName,
                                    packageName = mostFrequent.browserPackageName
                                )
                            )
                        } else {
                            DomainResult.Success(null)
                        }
                    }
                }
                is DomainResult.Failure -> DomainResult.Failure(result.error)
            }
        }
    }
}

class GetMostRecentlyUsedBrowserUseCaseImpl @Inject constructor(
    private val browserStatsRepository: BrowserStatsRepository
) : GetMostRecentlyUsedBrowserUseCase {
    override fun invoke(): Flow<DomainResult<BrowserAppInfo?, AppError>> {
        return browserStatsRepository.getAllBrowserStatsSortedByLastUsed().map { result ->
            when (result) {
                is DomainResult.Success -> {
                    val stats = result.data
                    if (stats.isEmpty()) {
                        DomainResult.Success(null)
                    } else {
                        // The first browser in the list is the most recently used
                        val mostRecent = stats.firstOrNull()
                        if (mostRecent != null) {
                            // In a real implementation, we would query for app name based on package name
                            val appName = mostRecent.browserPackageName.substringAfterLast('.', mostRecent.browserPackageName)

                            DomainResult.Success(
                                BrowserAppInfo(
                                    appName = appName,
                                    packageName = mostRecent.browserPackageName
                                )
                            )
                        } else {
                            DomainResult.Success(null)
                        }
                    }
                }
                is DomainResult.Failure -> DomainResult.Failure(result.error)
            }
        }
    }
}
