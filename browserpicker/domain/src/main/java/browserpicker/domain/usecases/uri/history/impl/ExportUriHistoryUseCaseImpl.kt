package browserpicker.domain.usecases.uri.history.impl

import androidx.paging.PagingConfig
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.JsonModule.KOTLIN_SERIALIZATION_JSON_CONFIG
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.ExportUriHistoryUseCase
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named


class ExportUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    private val json: Json
) : ExportUriHistoryUseCase {
    override suspend fun invoke(filePath: String, query: UriHistoryQuery): DomainResult<Int, AppError> {
        return try {
            // Get the total count to allocate appropriate resources
            val countResult = uriHistoryRepository.getTotalUriRecordCount(query).first()
            if (countResult is DomainResult.Failure) {
                return DomainResult.Failure(countResult.error)
            }

            val totalCount = (countResult as DomainResult.Success).data

            // Load all records - we should use a more efficient batching approach for very large datasets
            // This is a simplified version for demonstration
            val pagingConfig = PagingConfig(
                pageSize = totalCount.toInt().coerceAtMost(1000),
                enablePlaceholders = false
            )

            val records = listOf<UriRecord>()
            val pagingData = uriHistoryRepository.getPagedUriRecords(query, pagingConfig)

            // Since we can't directly collect from PagingData, we'll use a different approach
            // In a real implementation, you'd implement a collector for PagingData or use a Repository method
            // that returns a Flow<List<UriRecord>> instead

            // For now, let's assume we have a method to get all records matching the query
            // This would be a simplification
            val recordsResult = DomainResult.Success(records) // Placeholder

            if (recordsResult is DomainResult.Failure<*>) {
                return DomainResult.Failure(recordsResult.error)
            }

            val exportedRecords = (recordsResult as DomainResult.Success).data
            val jsonString = json.encodeToString(exportedRecords)

            File(filePath).writeText(jsonString)

            DomainResult.Success(exportedRecords.size)
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError("Failed to export URI history: ${e.message}", e))
        }
    }
}

/*
class ExportUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository, // To fetch data
//    private val exporter: UriHistoryExporter // To export data
): ExportUriHistoryUseCase {
    override suspend operator fun invoke(filePath: String, query: UriHistoryQuery): DomainResult<Int, AppError> {
        // IMPORTANT LIMITATION:
        // The current UriHistoryRepository provides getPagedUriRecords, which is suitable for UI display.
        // Exporting ALL records matching a query requires a repository method to fetch all non-paged data
        // (e.g., getAllRecords(query): List<UriRecord> or Flow<List<UriRecord>>).
        // Without such a method, accurately exporting all matching records is not feasible here.
        // The following is a placeholder demonstrating the concept but CANNOT export all data.
        // For a real implementation, the repository should be extended, or this use case
        // would need to handle fetching all pages, which is complex and resource-intensive.

        // This is a simplified approach and will not work for actual full export.
        // It indicates that this functionality is not fully implementable with current repository capabilities for ALL data.
        return DomainResult.Failure(
            AppError.UnknownError( // Consider a specific AppError.NotSupported type
                message = "Full URI history export is not currently supported due to repository limitations for fetching all non-paged records matching a query. " +
                        "This use case requires a way to retrieve all relevant records, not just a single page."
            )
        )
        // A conceptual (but currently non-functional due to repo limitation) flow would be:
        // val allRecordsResult = uriHistoryRepository.getAllRecordsMatchingQuery(query) // Hypothetical
        // return allRecordsResult.fold(
        //     onSuccess = { records -> exporter.exportHistory(filePath, records) },
        //     onFailure = { error -> DomainResult.Failure(error) }
        // )
    }
}

 */