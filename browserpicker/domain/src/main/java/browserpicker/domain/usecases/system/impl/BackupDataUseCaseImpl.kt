package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.*
import browserpicker.domain.usecases.system.BackupDataUseCase
import browserpicker.domain.usecases.system.model.BackupDataContainer
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BackupDataUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
    private val folderRepository: FolderRepository,
    private val browserStatsRepository: BrowserStatsRepository,
    // Consider injecting this Json instance via DI for consistency and testability
    private val json: Json
) : BackupDataUseCase {

    // This could be injected or defined elsewhere, e.g. BuildConfig
    private val currentAppVersion = "1.0.0" // Placeholder

    override suspend operator fun invoke(filePath: String, includeHistory: Boolean): DomainResult<Unit, AppError> {
        if (filePath.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("File path cannot be blank."))
        }

        return try {
            val uriRecordsToBackup = if (includeHistory) {
                // This fetches all records. For very large histories, consider paginated fetching
                // or a dedicated "getAll" method in the repository if not excessively large.
                // For simplicity, assuming a manageable direct fetch or an adapted repository method.
                // This part needs careful consideration based on actual data size and performance.
                // A simple approach: use a query that fetches all, then map the PagingData.
                // However, PagingData is for UI. Repository should offer a way to get all for backup.
                // Let's assume UriHistoryRepository has a (hypothetical) getAllUriRecords(): Flow<DomainResult<List<UriRecord>, AppError>>
                // For now, we'll simulate this with a placeholder, as current repo doesn't have it.
                // This highlights a potential need for change in UriHistoryRepository.
                 uriHistoryRepository.getDistinctHosts().map { it.getOrNull()?.size }.firstOrNull() // Placeholder logic
                 null // Placeholder: Actual fetching of all UriRecords needed here.
            } else {
                null
            }

            val hostRulesResult = hostRuleRepository.getAllHostRules().firstOrNull()
            val foldersResult = folderRepository.getAllFoldersByType(browserpicker.domain.model.FolderType.BOOKMARK).firstOrNull() // Example, might need all types
            val blockedFoldersResult = folderRepository.getAllFoldersByType(browserpicker.domain.model.FolderType.BLOCK).firstOrNull()
            val browserStatsResult = browserStatsRepository.getAllBrowserStats().firstOrNull()

            val hostRules = hostRulesResult?.getOrNull() ?: emptyList()
            val bookmarkFolders = foldersResult?.getOrNull() ?: emptyList()
            val blockedFolders = blockedFoldersResult?.getOrNull() ?: emptyList()
            val allFolders = (bookmarkFolders + blockedFolders).distinctBy { it.id }
            val browserStats = browserStatsResult?.getOrNull() ?: emptyList()

            // Check for errors during data fetching
            listOfNotNull(hostRulesResult, foldersResult, browserStatsResult).forEach {
                if (it?.isFailure == true) return DomainResult.Failure(it.errorOrNull() ?: AppError.UnknownError("Failed to fetch data for backup"))
            }
            // Add error check for uriRecordsToBackup if/when actual fetching is implemented

            val backupData = BackupDataContainer(
                appVersion = currentAppVersion,
                backupTimestamp = Clock.System.now(),
                uriRecords = uriRecordsToBackup, // This is currently null
                hostRules = hostRules,
                folders = allFolders,
                browserStats = browserStats
            )

            val backupJsonString = json.encodeToString(backupData)
            systemRepository.writeBackupFile(filePath, backupJsonString)
        } catch (e: kotlinx.serialization.SerializationException) {
            DomainResult.Failure(AppError.DataMappingError("Failed to serialize backup data: ${e.message}"))
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError("Failed to backup data: ${e.message}", cause = e))
        }
    }
} 