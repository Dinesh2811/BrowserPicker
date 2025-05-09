package browserpicker.domain.usecases

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.model.UriRecord
import browserpicker.domain.service.PagingDefaults
import kotlinx.coroutines.flow.Flow

interface GetPagedUriHistoryUseCase {
    /**
     * Gets paged URI history records based on query parameters
     */
    operator fun invoke(
        query: UriHistoryQuery = UriHistoryQuery.DEFAULT,
        pagingConfig: PagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG
    ): Flow<PagingData<UriRecord>>
}

interface GetUriHistoryCountUseCase {
    /**
     * Gets the total count of URI history records matching the query
     */
    operator fun invoke(query: UriHistoryQuery = UriHistoryQuery.DEFAULT): Flow<DomainResult<Long, AppError>>
}

interface GetUriHistoryGroupCountsUseCase {
    /**
     * Gets counts of URI records grouped by the query's groupBy parameter
     */
    operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>>
}

interface GetUriHistoryDateCountsUseCase {
    /**
     * Gets counts of URI records grouped by date
     */
    operator fun invoke(query: UriHistoryQuery): Flow<DomainResult<List<DateCount>, AppError>>
}

interface GetUriRecordByIdUseCase {
    /**
     * Gets a URI record by ID
     */
    suspend operator fun invoke(id: Long): DomainResult<UriRecord?, AppError>
}

interface DeleteUriRecordUseCase {
    /**
     * Deletes a URI record by ID
     */
    suspend operator fun invoke(id: Long): DomainResult<Unit, AppError>
}

interface DeleteAllUriHistoryUseCase {
    /**
     * Deletes all URI history records
     */
    suspend operator fun invoke(): DomainResult<Int, AppError>
}

interface GetUriFilterOptionsUseCase {
    /**
     * Gets options for filtering URI history (hosts, browsers)
     */
    operator fun invoke(): Flow<DomainResult<FilterOptions, AppError>>
}

interface ExportUriHistoryUseCase {
    /**
     * Exports URI history to a file
     */
    suspend operator fun invoke(filePath: String, query: UriHistoryQuery = UriHistoryQuery.DEFAULT): DomainResult<Int, AppError>
}

interface ImportUriHistoryUseCase {
    /**
     * Imports URI history from a file
     */
    suspend operator fun invoke(filePath: String): DomainResult<Int, AppError>
}
//
//interface GetDistinctHistoryHostsUseCase {
//    /**
//     * Gets all distinct hosts from URI history.
//     */
//    operator fun invoke(): Flow<DomainResult<List<String>, AppError>>
//}
//
//interface GetDistinctChosenBrowsersUseCase {
//    /**
//     * Gets all distinct chosen browser package names from URI history.
//     */
//    operator fun invoke(): Flow<DomainResult<List<String?>, AppError>>
//}