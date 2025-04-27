package browserpicker.data.local.repository

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig as AndroidPagingConfig // Alias to avoid name clash
import androidx.paging.PagingData
import androidx.paging.map
import browserpicker.data.local.dao.*
import browserpicker.data.local.entity.*
import browserpicker.data.local.mapper.FolderMapper.toBlockEntity
import browserpicker.data.local.mapper.FolderMapper.toBookmarkEntity
import browserpicker.data.local.mapper.FolderMapper.toDomainModel
import browserpicker.data.local.mapper.HostRuleMapper.toDomainModel
import browserpicker.data.local.mapper.HostRuleMapper.toEntity
import browserpicker.data.local.mapper.UriRecordMapper.toEntity
import browserpicker.domain.model.*
import browserpicker.domain.repository.BrowserPickerRepository
import browserpicker.domain.repository.SortColumn
import browserpicker.domain.repository.SortOrder
import browserpicker.domain.repository.SortPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import androidx.paging.PagingConfig
import browserpicker.data.local.mapper.UriRecordMapper.toDomainModel

@Singleton
class BrowserPickerRepositoryImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val hostRuleDao: HostRuleDao,
    private val bookmarkFolderDao: BookmarkFolderDao,
    private val blockFolderDao: BlockFolderDao,
    private val clock: Clock,
) : BrowserPickerRepository {

    private fun PagingConfig.toAndroidConfig(): AndroidPagingConfig {
        return AndroidPagingConfig(
            pageSize = this.pageSize,
            prefetchDistance = this.prefetchDistance,
            enablePlaceholders = this.enablePlaceholders,
            initialLoadSize = this.initialLoadSize,
            maxSize = this.maxSize
        )
    }

    // --- Host Rules ---

    override fun observeHostRule(host: String): Flow<Result<HostRule?>> = channelFlow {
        try {
            hostRuleDao.observeRuleByHost(host)
                .map { entity -> Result.success(entity?.toDomainModel()) } // Use mapper
                .collect { send(it) }
        } catch (e: Exception) {
            // Log error
            println("Error observing host rule for $host: ${e.message}")
            send(Result.failure(DataAccessException("Failed to observe host rule for $host", e))) // Wrap known exceptions
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getHostRule(host: String): Result<HostRule?> = withContext(Dispatchers.IO) {
        runCatching {
            hostRuleDao.getRuleByHost(host)?.toDomainModel() // Use mapper
        }.recoverCatching { e ->
            throw DataAccessException("Failed to get host rule for $host", e) // Wrap known exceptions
        }
    }

    override suspend fun saveHostRule(rule: HostRule): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()

            // --- Constraint Validation --- (Keep this logic here or move to a Use Case)
            if (rule.uriStatus == UriStatus.UNKNOWN) {
                throw InvalidRuleDataException("Cannot save rule with type UNKNOWN.")
            }
            if (rule.uriStatus == UriStatus.NONE && (rule.bookmarkFolderId != null || rule.blockFolderId != null)) {
                throw InvalidRuleDataException("Bookmark/Block folder ID must be null when RuleType is NONE.")
            }
            if (rule.uriStatus == UriStatus.BOOKMARKED && rule.blockFolderId != null) {
                throw InvalidRuleDataException("Block folder ID must be null when RuleType is BOOKMARK.")
            }
            if (rule.uriStatus == UriStatus.BLOCKED && rule.bookmarkFolderId != null) {
                throw InvalidRuleDataException("Bookmark folder ID must be null when RuleType is BLOCK.")
            }
            if (rule.host.isBlank()) {
                throw InvalidRuleDataException("Host cannot be empty.")
            }
            // --- End Constraint Validation ---

            val existingRule = hostRuleDao.getRuleByHost(rule.host) // Check for existing by host
            val entityToSave = rule.toEntity(currentTime)

            if (existingRule == null) {
                // Insert new rule
                hostRuleDao.insert(entityToSave) // Returns the new row ID
            } else {
                // Update existing rule - ensure we use the existing ID and preserve original createdAt
                val updatedEntity = entityToSave.copy(
                    id = existingRule.id,
                    createdAt = existingRule.createdAt // Preserve original creation time
                )
                hostRuleDao.update(updatedEntity)
                existingRule.id // Return the existing ID
            }
        }.recoverCatching { e ->
            // Provide more specific error types if possible
            when (e) {
                is SQLiteConstraintException -> throw HostRuleConstraintException("Host '${rule.host}' already exists or another constraint failed.", e)
                is InvalidRuleDataException -> throw e // Re-throw validation errors
                else -> throw DataAccessException("Failed to save host rule for ${rule.host}", e)
            }
        }
    }

    override suspend fun deleteHostRule(host: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affectedRows = hostRuleDao.deleteByHost(host)
            // Decide if you want to indicate if a rule wasn't found. Result.success(Unit) is fine even if 0 rows affected.
            // If you need to signal 'not found', use a different Result type like Result<Boolean> or a custom sealed class.
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete host rule for $host", e)
        }
    }

    override suspend fun deleteHostRuleById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affectedRows = hostRuleDao.deleteById(id)
            // Decide if you want to indicate if a rule wasn't found.
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete host rule with id $id", e)
        }
    }

    // --- Preferences ---

    override suspend fun setHostPreference(host: String, browserPackage: String?, isEnabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            val existingRule = hostRuleDao.getRuleByHost(host)

            if (existingRule != null) {
                // Update existing rule preference
                val updatedRuleEntity = existingRule.copy(
                    preferredBrowserPackage = browserPackage,
                    isPreferenceEnabled = isEnabled,
                    updatedAt = currentTime
                )
                hostRuleDao.update(updatedRuleEntity)
                Unit
            } else {
                // Create a new rule with ONLY the preference set (RuleType.NONE)
                // This could technically use the HostRule domain model and map it,
                // but creating the entity directly might be slightly simpler here
                // as we know the exact state (NONE ruleType, null folder IDs).
                val newRuleEntity = HostRuleEntity(
                    host = host,
                    uriStatus = UriStatus.NONE, // Default to NONE if only setting preference
                    preferredBrowserPackage = browserPackage,
                    isPreferenceEnabled = isEnabled,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    bookmarkFolderId = null,
                    blockFolderId = null
                )
                hostRuleDao.insert(newRuleEntity)
                Unit
            }
        }.recoverCatching { e ->
            when (e) {
                is SQLiteConstraintException -> throw HostRuleConstraintException("Constraint failed while setting preference for '$host'.", e)
                else -> throw DataAccessException("Failed to set preference for $host", e)
            }
        }
    }


    // --- Bookmarks ---

    override fun getBookmarkRulesPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<HostRule>> {
        return Pager(
            config = config.toAndroidConfig(),
            pagingSourceFactory = { // -> PagingSource<Int, HostRuleEntity> // Optional: explicit type hint here too if needed
                hostRuleDao.getRulesPagingSource(
                    ruleTypeValue = UriStatus.BOOKMARKED.value,
                    searchTerm = searchTerm,
                    isSortAsc = sort.order == SortOrder.ASCENDING
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() } // Use mapper
        }.flowOn(Dispatchers.IO)
    }

    override fun observeAllBookmarkFolders(): Flow<Result<List<Folder>>> = channelFlow {
        try {
            bookmarkFolderDao.observeAllFolders()
                .map { entities -> Result.success(entities.map { it.toDomainModel() }) } // Use mapper
                .collect { send(it) }
        } catch (e: Exception) {
            send(Result.failure(DataAccessException("Failed to observe bookmark folders", e)))
        }
    }.flowOn(Dispatchers.IO)


    override suspend fun saveBookmarkFolder(folder: Folder): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            // Type check moved to mapper for consistency, but can be here too
            val entity = folder.toBookmarkEntity(currentTime) // Use mapper

            if (entity.id == 0L) {
                bookmarkFolderDao.insert(entity) // Returns new ID
            } else {
                bookmarkFolderDao.update(entity) // Returns affected rows, we need the ID
                entity.id // Return the existing ID on update
            }
        }.recoverCatching { e ->
            // Check for specific exceptions like unique name constraints if applicable
            throw DataAccessException("Failed to save bookmark folder '${folder.name}'", e)
        }
    }

    override suspend fun deleteBookmarkFolder(folderId: Long, deleteContents: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            if (deleteContents) {
                // Find rules associated with this folder and delete them
                val rulesToDelete = hostRuleDao.getRulesByBookmarkFolder(folderId) // Get entities
                rulesToDelete.forEach { hostRuleDao.deleteById(it.id) } // Delete entities
            } else {
                // SET NULL should handle this via the FK constraint definition,
                // but clearing explicitly with DAO method is also an option if needed.
                hostRuleDao.clearBookmarkFolderId(folderId, currentTime)
            }

            // Handle nested folders: Set parentId to null for children.
            // Again, FK with SET NULL should handle this on delete.
            // This explicit call serves as a safeguard or alternative if FK isn't enough.
            bookmarkFolderDao.setChildrenParentToNull(folderId, currentTime)

            // Finally, delete the folder itself
            val affectedRows = bookmarkFolderDao.deleteById(folderId)
            if (affectedRows == 0) {
                // Optional: Log or indicate that the folder didn't exist
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete bookmark folder ID $folderId", e)
        }
    }

    // --- Blocks (Implementation similar to Bookmarks) ---

    override fun getBlockRulesPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<HostRule>> {
        return Pager(
            config = config.toAndroidConfig(),
            pagingSourceFactory = { // -> PagingSource<Int, HostRuleEntity> // Optional: explicit type hint here too if needed
                hostRuleDao.getRulesPagingSource(
                    ruleTypeValue = UriStatus.BLOCKED.value,
                    searchTerm = searchTerm,
                    isSortAsc = sort.order == SortOrder.ASCENDING
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() } // Use mapper
        }.flowOn(Dispatchers.IO)
    }

    override fun observeAllBlockFolders(): Flow<Result<List<Folder>>> = channelFlow {
        try {
            blockFolderDao.observeAllFolders()
                .map { entities -> Result.success(entities.map { it.toDomainModel() }) } // Use mapper
                .collect { send(it) }
        } catch (e: Exception) {
            send(Result.failure(DataAccessException("Failed to observe block folders", e)))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveBlockFolder(folder: Folder): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            val entity = folder.toBlockEntity(currentTime) // Use mapper

            if (entity.id == 0L) {
                blockFolderDao.insert(entity)
            } else {
                blockFolderDao.update(entity)
                entity.id
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to save block folder '${folder.name}'", e)
        }
    }

    override suspend fun deleteBlockFolder(folderId: Long, deleteContents: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            if (deleteContents) {
                val rulesToDelete = hostRuleDao.getRulesByBlockFolder(folderId)
                rulesToDelete.forEach { hostRuleDao.deleteById(it.id) }
            } else {
                hostRuleDao.clearBlockFolderId(folderId, currentTime) // Rely on FK SET NULL or explicit clear
            }

            blockFolderDao.setChildrenParentToNull(folderId, currentTime) // Handle children FKs

            val affectedRows = blockFolderDao.deleteById(folderId)
            if (affectedRows == 0) {
                // Optional: Log folder not found
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete block folder ID $folderId", e)
        }
    }


    // --- URI Interaction History ---

    override fun getHistoryPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<UriRecord>> {
        return Pager(
            config = config.toAndroidConfig(),
            pagingSourceFactory = {
                uriRecordDao.getHistoryPagingSource(
                    searchTerm = searchTerm,
                    // Adapt based on SortColumn if more complex sorting needed
                    isSortAsc = sort.order == SortOrder.ASCENDING && sort.column == SortColumn.TIMESTAMP // Simplified sorting
                )
                // --- Example using RawQuery ---
                // val query = SimpleSQLiteQuery(buildHistoryQuery(searchTerm, sort))
                // uriRecordDao.getHistoryPagingSourceRaw(query)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() } // Use mapper
        }.flowOn(Dispatchers.IO)
    }


    override suspend fun recordInteraction(interaction: UriRecord): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // Find associated host rule ID if not already present (maybe pass host instead?)
            val hostRuleId = interaction.hostRuleId ?: interaction.uriString.let { uri ->
                // Use the repository's own getUriHost method to handle potential errors
                getUriHost(uri).getOrNull()?.let { host ->
                    // Find the rule ID based on the extracted host
                    hostRuleDao.findHostRuleId(host)
                }
            }

            // Create entity, potentially copying with the found hostRuleId
            val entity = interaction.copy(hostRuleId = hostRuleId).toEntity() // Use mapper
            uriRecordDao.insert(entity) // Returns row ID
        }.recoverCatching { e ->
            throw DataAccessException("Failed to record interaction for ${interaction.uriString}", e)
        }
    }

    // Helper for RawQuery example (not used by default above)
    private fun buildHistoryQuery(searchTerm: String?, sort: SortPreference): String {
        val whereClause = if (searchTerm.isNullOrBlank()) "" else "WHERE uri_string LIKE '%${searchTerm.replace("'", "''")}%'"
        val orderByClause = when(sort.column) {
            SortColumn.TIMESTAMP -> "ORDER BY timestamp ${if(sort.order == SortOrder.ASCENDING) "ASC" else "DESC"}"
            // Add other columns
            else -> "ORDER BY timestamp DESC" // Default sort
        }
        return "SELECT * FROM uri_records $whereClause $orderByClause"
    }


    override suspend fun clearAllHistory(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            uriRecordDao.clearAllHistory()
        }.recoverCatching { e ->
            throw DataAccessException("Failed to clear all history", e)
        }
    }

    override suspend fun deleteHistoryRecord(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affectedRows = uriRecordDao.deleteById(id)
            // Decide if you want to indicate if a record wasn't found.
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete history record with id $id", e)
        }
    }

    // --- Analysis / Utils ---

    override suspend fun getInteractionCounts(since: Instant): Result<Map<InteractionAction, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            uriRecordDao.getInteractionCountsSince(since)
                .associate { it.action to it.count }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to get interaction counts since $since", e)
        }
    }

    // Simple host extraction utility
    override suspend fun getUriHost(uriString: String): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val host = Uri.parse(uriString).host
            if (host.isNullOrBlank()) {
                // Throw a specific domain-level exception or return a specific failure type
                throw IllegalArgumentException("Could not extract host from URI: $uriString")
            }
            host
        }.recoverCatching { e ->
            // Wrap known exceptions from URI parsing
            throw UriParsingException("Failed to parse URI or extract host from $uriString", e)
        }
    }
}

// --- Custom Exception Classes (Add UriParsingException) ---

open class DataAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class HostRuleConstraintException(message: String, cause: Throwable? = null) : DataAccessException(message, cause)
class InvalidRuleDataException(message: String) : IllegalArgumentException(message)
class UriParsingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


/*


import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig as AndroidPagingConfig // Alias to avoid name clash
import androidx.paging.PagingData
import androidx.paging.map
import browserpicker.data.local.dao.*
import browserpicker.data.local.entity.*
import browserpicker.data.local.mapper.FolderMapper.toBlockEntity
import browserpicker.data.local.mapper.FolderMapper.toBookmarkEntity
import browserpicker.data.local.mapper.FolderMapper.toDomainModel
import browserpicker.data.local.mapper.HostRuleMapper.toDomainModel
import browserpicker.data.local.mapper.HostRuleMapper.toEntity
import browserpicker.data.local.mapper.UriRecordMapper.toEntity
import browserpicker.domain.model.*
import browserpicker.domain.repository.BrowserPickerRepository
import browserpicker.domain.repository.SortColumn
import browserpicker.domain.repository.SortOrder
import browserpicker.domain.repository.SortPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import browserpicker.data.local.mapper.UriRecordMapper
import browserpicker.data.local.mapper.UriRecordMapper.toDomainModel


@Singleton
class BrowserPickerRepositoryImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val hostRuleDao: HostRuleDao,
    private val bookmarkFolderDao: BookmarkFolderDao,
    private val blockFolderDao: BlockFolderDao,
    private val clock: Clock, // Injected Clock
) : BrowserPickerRepository {

    // Helper for converting domain PagingConfig to Android PagingConfig
    private fun PagingConfig.toAndroidConfig(): AndroidPagingConfig {
        return AndroidPagingConfig(
            pageSize = this.pageSize,
            prefetchDistance = this.prefetchDistance,
            enablePlaceholders = this.enablePlaceholders,
            initialLoadSize = this.initialLoadSize,
            maxSize = this.maxSize
        )
    }

    // --- Host Rules ---

    override fun observeHostRule(host: String): Flow<Result<HostRule?>> = channelFlow {
        try {
            hostRuleDao.observeRuleByHost(host)
                .map { entity -> Result.success(entity?.toDomainModel()) }
                .collect { send(it) }
        } catch (e: Exception) {
            // Log error (consider a proper logging framework)
            println("Error observing host rule for $host: ${e.message}")
            send(Result.failure(e)) // Emit failure
        }
    }.flowOn(Dispatchers.IO) // Ensure DAO access is off the main thread


    override suspend fun getHostRule(host: String): Result<HostRule?> = withContext(Dispatchers.IO) {
        runCatching {
            hostRuleDao.getRuleByHost(host)?.toDomainModel()
        }
    }

    override suspend fun saveHostRule(rule: HostRule): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()

            // --- Constraint Validation ---
            if (rule.ruleType == RuleType.UNKNOWN) {
                throw InvalidRuleDataException("Cannot save rule with type UNKNOWN.")
            }
            if (rule.ruleType == RuleType.NONE && (rule.bookmarkFolderId != null || rule.blockFolderId != null)) {
                throw InvalidRuleDataException("Bookmark/Block folder ID must be null when RuleType is NONE.")
            }
            if (rule.ruleType == RuleType.BOOKMARK && rule.blockFolderId != null) {
                throw InvalidRuleDataException("Block folder ID must be null when RuleType is BOOKMARK.")
            }
            if (rule.ruleType == RuleType.BLOCK && rule.bookmarkFolderId != null) {
                throw InvalidRuleDataException("Bookmark folder ID must be null when RuleType is BLOCK.")
            }
            if (rule.host.isBlank()) {
                throw InvalidRuleDataException("Host cannot be empty.")
            }
            // --- End Constraint Validation ---

            val entity = rule.toEntity(currentTime)
            val existingRule = hostRuleDao.getRuleByHost(entity.host)

            if (existingRule == null) {
                // Insert new rule
                hostRuleDao.insert(entity) // Returns the new row ID
            } else {
                // Update existing rule - ensure we keep the original createdAt time
                val updatedEntity = entity.copy(
                    id = existingRule.id,
                    createdAt = existingRule.createdAt // Preserve original creation time
                )
                hostRuleDao.update(updatedEntity)
                existingRule.id // Return the existing ID
            }
        }.recoverCatching { e ->
            // Provide more specific error types if possible
            when (e) {
                is SQLiteConstraintException -> throw HostRuleConstraintException("Host '${rule.host}' already exists or another constraint failed.", e)
                is InvalidRuleDataException -> throw e // Re-throw validation errors
                else -> throw DataAccessException("Failed to save host rule for ${rule.host}", e)
            }
        }
    }


    override suspend fun deleteHostRule(host: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affectedRows = hostRuleDao.deleteByHost(host)
            if (affectedRows == 0) {
                // Optional: Throw if you expect a rule to always exist before deletion
                // throw NoSuchElementException("No rule found for host: $host")
            }
        }
    }

    override suspend fun deleteHostRuleById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affectedRows = hostRuleDao.deleteById(id)
            if (affectedRows == 0) {
                // Optional: Throw if you expect a rule to always exist before deletion
                // throw NoSuchElementException("No rule found for id: $id")
            }
        }
    }

    // --- Preferences ---

    override suspend fun setHostPreference(host: String, browserPackage: String?, isEnabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            val existingRule = hostRuleDao.getRuleByHost(host)

            if (existingRule != null) {
                // Update existing rule preference
                val updatedRuleEntity = existingRule.copy(
                    preferredBrowserPackage = browserPackage,
                    isPreferenceEnabled = isEnabled,
                    updatedAt = currentTime
                )
                hostRuleDao.update(updatedRuleEntity)
                Unit
            } else {
                // Create a new rule with ONLY the preference set (RuleType.NONE)
                // This could technically use the HostRule domain model and map it,
                // but creating the entity directly might be slightly simpler here
                // as we know the exact state (NONE ruleType, null folder IDs).
                val newRuleEntity = HostRuleEntity(
                    host = host,
                    ruleType = RuleType.NONE, // Default to NONE if only setting preference
                    preferredBrowserPackage = browserPackage,
                    isPreferenceEnabled = isEnabled,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    bookmarkFolderId = null,
                    blockFolderId = null
                )
                hostRuleDao.insert(newRuleEntity)
                Unit
            }
        }.recoverCatching { e ->
            when (e) {
                is SQLiteConstraintException -> throw HostRuleConstraintException("Constraint failed while setting preference for '$host'.", e)
                else -> throw DataAccessException("Failed to set preference for $host", e)
            }
        }
    }

    // --- Bookmarks ---

    override fun getBookmarkRulesPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<HostRule>> {
        // Basic implementation, ignoring sort for now, add later if needed via RawQuery
        return Pager(
            config = config.toAndroidConfig(),
            pagingSourceFactory = {
                hostRuleDao.getRulesPagingSource(
                    ruleTypeValue = RuleType.BOOKMARK.value,
                    searchTerm = searchTerm,
                    isSortAsc = sort.order == SortOrder.ASCENDING // Simplified sorting
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeAllBookmarkFolders(): Flow<Result<List<Folder>>> = channelFlow {
        try {
            bookmarkFolderDao.observeAllFolders()
                .map { entities -> Result.success(entities.map { it.toDomainModel() }) }
                .collect { send(it) }
        } catch (e: Exception) {
            send(Result.failure(DataAccessException("Failed to observe bookmark folders", e)))
        }
    }.flowOn(Dispatchers.IO)


    override suspend fun saveBookmarkFolder(folder: Folder): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            if (folder.type != FolderType.BOOKMARK) throw IllegalArgumentException("Folder type must be BOOKMARK")
            val entity = folder.toBookmarkEntity(clock.now())
            if (entity.id == 0L) {
                bookmarkFolderDao.insert(entity)
            } else {
                bookmarkFolderDao.update(entity)
                entity.id // Return existing ID on update
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to save bookmark folder '${folder.name}'", e)
        }
    }

    override suspend fun deleteBookmarkFolder(folderId: Long, deleteContents: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            if (deleteContents) {
                // Find rules associated with this folder and delete them
                val rulesToDelete = hostRuleDao.getRulesByBookmarkFolder(folderId)
                rulesToDelete.forEach { hostRuleDao.deleteById(it.id) }
            } else {
                // Option 1: Set folderId to null on associated rules (using SET NULL in FK is better)
                hostRuleDao.clearBookmarkFolderId(folderId, currentTime)

                // Option 2: Explicitly update rules (more control but SET NULL in FK is preferred)
                // val rulesToUpdate = hostRuleDao.getRulesByBookmarkFolder(folderId)
                // rulesToUpdate.forEach { rule ->
                //     hostRuleDao.update(rule.copy(bookmarkFolderId = null, updatedAt = currentTime))
                // }
            }

            // Handle nested folders: Set parentId to null for children
            // This should ideally be handled by ForeignKey(onDelete = SET_NULL) in the entity,
            // but we might need explicit handling if cascade behavior is complex.
            // The DAO `setChildrenParentToNull` helps if explicit update is needed.
            bookmarkFolderDao.setChildrenParentToNull(folderId, currentTime)


            // Finally, delete the folder itself
            val affectedRows = bookmarkFolderDao.deleteById(folderId)
            if (affectedRows == 0) {
                // Optional: Log or indicate that the folder didn't exist
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete bookmark folder ID $folderId", e)
        }
    }


    // --- Blocks (Implementation similar to Bookmarks) ---

    override fun getBlockRulesPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<HostRule>> {
        return Pager(
            config = config.toAndroidConfig(),
            pagingSourceFactory = {
                hostRuleDao.getRulesPagingSource(
                    ruleTypeValue = RuleType.BLOCK.value,
                    searchTerm = searchTerm,
                    isSortAsc = sort.order == SortOrder.ASCENDING
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeAllBlockFolders(): Flow<Result<List<Folder>>> = channelFlow {
        try {
            blockFolderDao.observeAllFolders()
                .map { entities -> Result.success(entities.map { it.toDomainModel() }) }
                .collect { send(it) }
        } catch (e: Exception) {
            send(Result.failure(DataAccessException("Failed to observe block folders", e)))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveBlockFolder(folder: Folder): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            if (folder.type != FolderType.BLOCK) throw IllegalArgumentException("Folder type must be BLOCK")
            val entity = folder.toBlockEntity(clock.now())
            if (entity.id == 0L) {
                blockFolderDao.insert(entity)
            } else {
                blockFolderDao.update(entity)
                entity.id
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to save block folder '${folder.name}'", e)
        }
    }

    override suspend fun deleteBlockFolder(folderId: Long, deleteContents: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentTime = clock.now()
            if (deleteContents) {
                val rulesToDelete = hostRuleDao.getRulesByBlockFolder(folderId)
                rulesToDelete.forEach { hostRuleDao.deleteById(it.id) }
            } else {
                hostRuleDao.clearBlockFolderId(folderId, currentTime) // Rely on FK SET NULL or explicit clear
            }

            blockFolderDao.setChildrenParentToNull(folderId, currentTime) // Handle children FKs

            val affectedRows = blockFolderDao.deleteById(folderId)
            if (affectedRows == 0) {
                // Optional: Log folder not found
            }
        }.recoverCatching { e ->
            throw DataAccessException("Failed to delete block folder ID $folderId", e)
        }
    }


    // --- URI Interaction History ---

    override suspend fun recordInteraction(interaction: UriRecord): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // Find associated host rule ID if not already present (maybe pass host instead?)
            val hostRuleId = interaction.hostRuleId ?: interaction.uriString.let { uri ->
                getUriHost(uri).getOrNull()?.let { host ->
                    hostRuleDao.findHostRuleId(host)
                }
            }

            val entity = interaction.copy(hostRuleId = hostRuleId).toEntity()
            uriRecordDao.insert(entity)
        }.recoverCatching { e ->
            throw DataAccessException("Failed to record interaction for ${interaction.uriString}", e)
        }
    }


    override fun getHistoryPagingData(config: PagingConfig, searchTerm: String?, sort: SortPreference): Flow<PagingData<UriRecord>> {
        return Pager(
            config = config.toAndroidConfig(),
            pagingSourceFactory = {
                uriRecordDao.getHistoryPagingSource(
                    searchTerm = searchTerm,
                    // Adapt based on SortColumn if more complex sorting needed
                    isSortAsc = sort.order == SortOrder.ASCENDING && sort.column == SortColumn.TIMESTAMP // Simplified sorting
                )
                // --- Example using RawQuery ---
                // val query = SimpleSQLiteQuery(buildHistoryQuery(searchTerm, sort))
                // uriRecordDao.getHistoryPagingSourceRaw(query)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() } // Use mapper
        }.flowOn(Dispatchers.IO)
    }

    // Helper for RawQuery example (not used by default above)
    private fun buildHistoryQuery(searchTerm: String?, sort: SortPreference): String {
        val whereClause = if (searchTerm.isNullOrBlank()) "" else "WHERE uri_string LIKE '%${searchTerm.replace("'", "''")}%'" // Basic sanitation
        val orderByClause = when(sort.column) {
            SortColumn.TIMESTAMP -> "ORDER BY timestamp ${if(sort.order == SortOrder.ASCENDING) "ASC" else "DESC"}"
            // Add other columns
            else -> "ORDER BY timestamp DESC" // Default sort
        }
        return "SELECT * FROM uri_records $whereClause $orderByClause"
    }


    override suspend fun clearAllHistory(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            uriRecordDao.clearAllHistory()
        }
    }

    override suspend fun deleteHistoryRecord(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affectedRows = uriRecordDao.deleteById(id)
            if (affectedRows == 0) {
                // Optional: throw NoSuchElementException("History record not found: $id")
            }
        }
    }

    // --- Analysis / Utils ---

    override suspend fun getInteractionCounts(since: Instant): Result<Map<InteractionAction, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            uriRecordDao.getInteractionCountsSince(since)
                .associate { it.action to it.count } // Convert list of tuples to Map
        }
    }

    // Simple host extraction utility
    override suspend fun getUriHost(uriString: String): Result<String> = withContext(Dispatchers.Default) { // Use Default dispatcher for CPU-bound parsing
        runCatching {
            val host = uriString.toUri().host
            if (host.isNullOrBlank()) {
                throw IllegalArgumentException("Could not extract host from URI: $uriString")
            }
            host
        }
    }
}

// --- Custom Exception Classes (Place in data/exception or similar) ---

open class DataAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class HostRuleConstraintException(message: String, cause: Throwable? = null) : DataAccessException(message, cause)
class InvalidRuleDataException(message: String) : IllegalArgumentException(message)
class UriParsingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

 */