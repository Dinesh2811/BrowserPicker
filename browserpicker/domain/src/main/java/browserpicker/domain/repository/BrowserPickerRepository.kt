package browserpicker.domain.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import browserpicker.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface UriHistoryRepository {
    //  TODO("Not yet implemented")
//    fun getPaginatedUriRecord(config: LogQueryConfig): Flow<PagingData<UriRecord>>
//    fun getTotalUriRecordCount(config: LogQueryConfig): Flow<Int>
//    fun getGroupedUriRecordCounts(config: LogQueryConfig): Flow<Map<GroupKey, Int>>
//    fun getDistinctBrowserPackages(): Flow<List<String>>
}
interface HostRuleRepository {
    //  TODO("Not yet implemented")
}
interface FolderRepository {
    //  TODO("Not yet implemented")
}
interface BrowserStatsRepository {
    //  TODO("Not yet implemented")
}

interface BrowserPickerRepository {

    // --- Host Rules ---
    fun observeHostRule(host: String): Flow<Result<HostRule?>>
    suspend fun getHostRule(host: String): Result<HostRule?>
    suspend fun saveHostRule(rule: HostRule): Result<Long> // Returns ID or error
    suspend fun deleteHostRule(host: String): Result<Unit>
    suspend fun deleteHostRuleById(id: Long): Result<Unit>

    // --- Preferences ---
    suspend fun setHostPreference(host: String, browserPackage: String?, isEnabled: Boolean): Result<Unit>

    // --- Bookmarks ---
    fun getBookmarkRulesPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<HostRule>>
    fun observeAllBookmarkFolders(): Flow<Result<List<Folder>>> // Assuming a common Folder domain model
    suspend fun saveBookmarkFolder(folder: Folder): Result<Long>
    suspend fun deleteBookmarkFolder(folderId: Long, deleteContents: Boolean): Result<Unit>

    // --- Blocks ---
    fun getBlockRulesPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<HostRule>>
    fun observeAllBlockFolders(): Flow<Result<List<Folder>>>
    suspend fun saveBlockFolder(folder: Folder): Result<Long>
    suspend fun deleteBlockFolder(folderId: Long, deleteContents: Boolean): Result<Unit>

    // --- URI Interaction History ---
    suspend fun recordInteraction(interaction: UriRecord): Result<Long>
    fun getHistoryPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<UriRecord>>
    suspend fun clearAllHistory(): Result<Int>
    suspend fun deleteHistoryRecord(id: Long): Result<Unit>

    // --- Analysis / Utils ---
    suspend fun getInteractionCounts(since: Instant): Result<Map<InteractionAction, Int>>
    suspend fun getUriHost(uriString: String): Result<String> // Utility maybe needed
}

// Helper domain models/enums for repository parameters (place in domain/model)
//data class PagingConfig(
//    val pageSize: Int = 20,
//    val prefetchDistance: Int = 5,
//    val enablePlaceholders: Boolean = false,
//    val initialLoadSize: Int = 60, // Often 3 * pageSize
//    val maxSize: Int = PagingConfig.MAX_SIZE_UNBOUNDED
//)

// Example Sort Preference - Adapt as needed
data class SortPreference(
    val column: SortColumn,
    val order: SortOrder
)

enum class SortColumn { TIMESTAMP, HOST, URI_STRING /* ... other relevant columns */ }
enum class SortOrder { ASCENDING, DESCENDING }
