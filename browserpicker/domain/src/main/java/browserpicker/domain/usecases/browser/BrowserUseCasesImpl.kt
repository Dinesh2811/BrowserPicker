package browserpicker.domain.usecases.browser

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.query.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

// Stub implementations for demonstration

class GetAvailableBrowsersUseCaseImpl @Inject constructor(): GetAvailableBrowsersUseCase {
    override operator fun invoke(): Flow<DomainResult<List<BrowserAppInfo>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetPreferredBrowserForHostUseCaseImpl @Inject constructor(): GetPreferredBrowserForHostUseCase {
    override operator fun invoke(host: String): Flow<DomainResult<BrowserAppInfo?, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class SetPreferredBrowserForHostUseCaseImpl @Inject constructor(): SetPreferredBrowserForHostUseCase {
    override suspend operator fun invoke(host: String, packageName: String, isEnabled: Boolean): DomainResult<Unit, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class ClearPreferredBrowserForHostUseCaseImpl @Inject constructor(): ClearPreferredBrowserForHostUseCase {
    override suspend operator fun invoke(host: String): DomainResult<Unit, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class RecordBrowserUsageUseCaseImpl @Inject constructor(): RecordBrowserUsageUseCase {
    override suspend operator fun invoke(packageName: String): DomainResult<Unit, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class GetBrowserUsageStatsUseCaseImpl @Inject constructor(): GetBrowserUsageStatsUseCase {
    override operator fun invoke(sortBy: BrowserStatSortField, sortOrder: SortOrder): Flow<DomainResult<List<BrowserUsageStat>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetBrowserUsageStatUseCaseImpl @Inject constructor(): GetBrowserUsageStatUseCase {
    override operator fun invoke(packageName: String): Flow<DomainResult<BrowserUsageStat?, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetMostFrequentlyUsedBrowserUseCaseImpl @Inject constructor(): GetMostFrequentlyUsedBrowserUseCase {
    override operator fun invoke(): Flow<DomainResult<BrowserAppInfo?, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetMostRecentlyUsedBrowserUseCaseImpl @Inject constructor(): GetMostRecentlyUsedBrowserUseCase {
    override operator fun invoke(): Flow<DomainResult<BrowserAppInfo?, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}
//
//class DeleteBrowserStatUseCaseImpl @Inject constructor(
//    private val browserStatsRepository: BrowserStatsRepository
//) : DeleteBrowserStatUseCase {
//    override suspend operator fun invoke(packageName: String): DomainResult<Unit, AppError> {
//        return browserStatsRepository.deleteBrowserStat(packageName)
//    }
//}
//
//class DeleteAllStatsUseCaseImpl @Inject constructor(
//    private val browserStatsRepository: BrowserStatsRepository
//) : DeleteAllStatsUseCase {
//    override suspend operator fun invoke(): DomainResult<Unit, AppError> {
//        return browserStatsRepository.deleteAllStats()
//    }
//}
