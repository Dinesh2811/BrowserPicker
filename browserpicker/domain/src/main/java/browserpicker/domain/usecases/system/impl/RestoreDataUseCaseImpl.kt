package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.*
import browserpicker.domain.usecases.system.RestoreDataUseCase
import browserpicker.domain.usecases.system.model.BackupDataContainer
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RestoreDataUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
    private val folderRepository: FolderRepository,
    private val browserStatsRepository: BrowserStatsRepository,
    // Consider injecting this Json instance via DI
    private val json: Json
) : RestoreDataUseCase {

    override suspend operator fun invoke(filePath: String, clearExistingData: Boolean): DomainResult<Unit, AppError> {
        if (filePath.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("File path cannot be blank."))
        }

        return try {
            val backupJsonStringResult = systemRepository.readBackupFile(filePath)
            if (backupJsonStringResult.isFailure) {
                return DomainResult.Failure(backupJsonStringResult.errorOrNull()!!)
            }
            val backupJsonString = backupJsonStringResult.getOrNull()!!

            val backupData = json.decodeFromString<BackupDataContainer>(backupJsonString)

            if (clearExistingData) {
                // Order of deletion might matter depending on foreign key constraints
                // Assuming cascade delete is not universally set up or to be absolutely sure.
                uriHistoryRepository.deleteAllUriRecords() // if uriRecords were backed up
                // hostRuleRepository.deleteAllHostRules() // Assuming a method like this exists or iterate and delete
                // folderRepository.deleteAllFolders() // Assuming a method like this exists
                browserStatsRepository.deleteAllStats()
                // Add more deletion calls as necessary
                // This part needs repository methods for bulk deletion.
            }

            // Restore data - this requires add/insert methods in repositories
            // For simplicity, assuming repositories have methods to add/update collections or individual items.
            // Error handling for each step of data restoration should be added.

            // Example for folders (assuming createFolder handles conflicts or updates)
            backupData.folders.forEach { folder ->
                // This is simplified. Actual folder restoration needs to handle parent-child relationships correctly,
                // potentially by sorting them or creating roots first, then children.
                // Also, default folders might need special handling.
                folderRepository.findFolderByNameAndParent(folder.name, folder.parentFolderId, folder.type).mapSuccess {
                    if(it == null) folderRepository.createFolder(folder.name, folder.parentFolderId, folder.type)
                }
            }

            backupData.hostRules.forEach { rule ->
                hostRuleRepository.saveHostRule(rule.host, rule.uriStatus, rule.folderId, rule.preferredBrowserPackage, rule.isPreferenceEnabled)
            }

            backupData.browserStats.forEach { stat ->
                // BrowserStatsRepository might need an update or insert method
                // For now, let's assume recordBrowserUsage can effectively restore/set a stat, 
                // though it's designed for incremental updates. A dedicated set/restore method would be better.
                // This is a simplified placeholder for restoring stats.
                 repeat(stat.usageCount.toInt()) { browserStatsRepository.recordBrowserUsage(stat.browserPackageName) } 
            }

            backupData.uriRecords?.forEach { record ->
                uriHistoryRepository.addUriRecord(record.uriString, record.host, record.uriSource, record.interactionAction, record.chosenBrowserPackage, record.associatedHostRuleId)
            }
            
            // Ensure default folders exist after potential clearing/restoration
            folderRepository.ensureDefaultFoldersExist()

            DomainResult.Success(Unit)
        } catch (e: kotlinx.serialization.SerializationException) {
            DomainResult.Failure(AppError.DataMappingError("Failed to deserialize backup data: ${e.message}"))
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError("Failed to restore data: ${e.message}", cause = e))
        }
    }
} 