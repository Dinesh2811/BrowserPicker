package browserpicker.domain.usecases.uri.history.model

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource

/**
 * Data Transfer Object for importing URI records.
 * This structure is used because imported records might not have existing IDs,
 * and timestamps are typically assigned upon creation.
 */
data class UriRecordImportData(
    val uriString: String,
    val host: String,
    val source: UriSource,
    val action: InteractionAction,
    val chosenBrowser: String?,
    // Consider adding originalTimestamp if it needs to be preserved during import
    // AssociatedHostRuleId is omitted as resolving it during import can be complex
)

/**
 * Interface for exporting URI history.
 * Implementations will handle the specifics of writing data to a file.
 */
interface UriHistoryExporter {
    /**
     * Exports the given list of URI records to the specified file path.
     * @param filePath The path to the file where history should be exported.
     * @param records The list of UriRecord entities to export.
     * @return DomainResult holding the count of successfully exported records, or an AppError.
     */
    suspend fun exportHistory(filePath: String, records: List<UriRecord>): DomainResult<Int, AppError>
}

/**
 * Interface for importing URI history.
 * Implementations will handle reading and parsing data from a file.
 */
interface UriHistoryImporter {
    /**
     * Imports URI records from the specified file path.
     * @param filePath The path to the file from which history should be imported.
     * @return DomainResult holding a list of [UriRecordImportData] ready to be processed and stored,
     *         or an AppError if file reading or parsing fails.
     */
    suspend fun importHistory(filePath: String): DomainResult<List<UriRecordImportData>, AppError>
}
