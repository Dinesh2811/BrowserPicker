package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.ImportUriHistoryUseCase
import javax.inject.Inject

class ImportUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
//    private val importer: UriHistoryImporter
): ImportUriHistoryUseCase {
    override suspend fun invoke(filePath: String): DomainResult<Int, AppError> {
        TODO("Not yet implemented")
    }
    //    override suspend operator fun invoke(filePath: String): DomainResult<Int, AppError> {
//        val importResult = importer.importHistory(filePath)
//
//        return importResult.fold(
//            onSuccess = { recordsToImport ->
//                var successfulImports = 0
//                var firstError: AppError? = null
//
//                for (recordData in recordsToImport) {
//                    val addResult = uriHistoryRepository.addUriRecord(
//                        uriString = recordData.uriString,
//                        host = recordData.host,
//                        source = recordData.source,
//                        action = recordData.action,
//                        chosenBrowser = recordData.chosenBrowser
//                        // associatedHostRuleId is not part of UriRecordImportData for simplicity
//                    )
//                    if (addResult.isSuccess) {
//                        successfulImports++
//                    } else if (firstError == null) {
//                        firstError = addResult.errorOrNull()
//                    }
//                }
//
//                if (successfulImports > 0 && firstError == null) {
//                    DomainResult.Success(successfulImports)
//                } else if (successfulImports == 0 && firstError != null) {
//                    DomainResult.Failure(firstError) // No records imported, report first error
//                } else if (successfulImports > 0 && firstError != null) {
//                    // Partial success, report count but also indicate issues
//                    DomainResult.Failure(
//                        AppError.DataIntegrityError(
//                            message = "Partially imported $successfulImports records. First error: ${firstError.message}",
//                            cause = firstError.cause
//                        )
//                    )
//                } else if (recordsToImport.isEmpty()) {
//                    DomainResult.Success(0) // No records to import from file
//                } else {
//                    // Should not be reached if logic is correct
//                    DomainResult.Failure(AppError.UnknownError("Unknown error during import processing"))
//                }
//            },
//            onFailure = { error ->
//                DomainResult.Failure(error) // Error from importer (e.g., file not found, parsing error)
//            }
//        )
//    }
}
