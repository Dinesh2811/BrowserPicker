package browserpicker.domain.usecases.uri.history.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.JsonModule.KOTLIN_SERIALIZATION_JSON_CONFIG
import browserpicker.domain.model.UriRecord
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.usecases.uri.history.ImportUriHistoryUseCase
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class ImportUriHistoryUseCaseImpl @Inject constructor(
    private val uriHistoryRepository: UriHistoryRepository,
    @Named(KOTLIN_SERIALIZATION_JSON_CONFIG) private val json: Json
) : ImportUriHistoryUseCase {
    override suspend fun invoke(filePath: String): DomainResult<Int, AppError> {
        return try {
            val jsonString = File(filePath).readText()
            val records = json.decodeFromString<List<UriRecord>>(jsonString)

            var successCount = 0
            for (record in records) {
                // We need to insert each record individually
                // In a real implementation, consider batch operations if the repository supports them
                val result = uriHistoryRepository.addUriRecord(
                    uriString = record.uriString,
                    host = record.host,
                    source = record.uriSource,
                    action = record.interactionAction,
                    chosenBrowser = record.chosenBrowserPackage,
                    associatedHostRuleId = record.associatedHostRuleId
                )

                if (result is DomainResult.Success) {
                    successCount++
                }
            }

            DomainResult.Success(successCount)
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError("Failed to import URI history: ${e.message}", e))
        }
    }
}

/*
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

 */
