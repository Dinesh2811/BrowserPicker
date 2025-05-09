package browserpicker.domain.usecases.impl

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.Folder
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.*
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant
import javax.inject.Inject
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.domain.usecases.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.BrowserUsageReport
import browserpicker.domain.usecases.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.GetTopActionsByHostUseCase
import browserpicker.domain.usecases.SearchFoldersUseCase
import browserpicker.domain.usecases.SearchHostRulesUseCase
import browserpicker.domain.usecases.UriHistoryReport

class AnalyzeUriTrendsUseCaseImpl @Inject constructor(): AnalyzeUriTrendsUseCase {
    override operator fun invoke(timeRange: Pair<Instant, Instant>?): Flow<DomainResult<Map<String, List<DateCount>>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class AnalyzeBrowserUsageTrendsUseCaseImpl @Inject constructor(): AnalyzeBrowserUsageTrendsUseCase {
    override operator fun invoke(timeRange: Pair<Instant, Instant>?): Flow<DomainResult<Map<String, List<DateCount>>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetMostVisitedHostsUseCaseImpl @Inject constructor(): GetMostVisitedHostsUseCase {
    override operator fun invoke(limit: Int, timeRange: Pair<Instant, Instant>?): Flow<DomainResult<List<GroupCount>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GetTopActionsByHostUseCaseImpl @Inject constructor(): GetTopActionsByHostUseCase {
    override operator fun invoke(host: String): Flow<DomainResult<List<GroupCount>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class SearchHostRulesUseCaseImpl @Inject constructor(): SearchHostRulesUseCase {
    override operator fun invoke(
        query: String,
        filterByStatus: UriStatus?,
        includeFolderId: Long?,
        pagingConfig: PagingConfig
    ): Flow<PagingData<HostRule>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class SearchFoldersUseCaseImpl @Inject constructor(): SearchFoldersUseCase {
    override operator fun invoke(query: String, folderType: FolderType?): Flow<DomainResult<List<Folder>, AppError>> {
        // TODO: Implement logic
        return emptyFlow()
    }
}

class GenerateHistoryReportUseCaseImpl @Inject constructor(): GenerateHistoryReportUseCase {
    override suspend operator fun invoke(
        timeRange: Pair<Instant, Instant>?,
        exportToFile: Boolean,
        filePath: String?
    ): DomainResult<UriHistoryReport, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}

class GenerateBrowserUsageReportUseCaseImpl @Inject constructor(): GenerateBrowserUsageReportUseCase {
    override suspend operator fun invoke(
        timeRange: Pair<Instant, Instant>?,
        exportToFile: Boolean,
        filePath: String?
    ): DomainResult<BrowserUsageReport, AppError> {
        // TODO: Implement logic
        return DomainResult.Failure(AppError.UnknownError("Not implemented"))
    }
}