package browserpicker.domain.usecases.uri.host

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow

interface GetHostRuleUseCase {
    /**
     * Gets a host rule by host
     */
    operator fun invoke(host: String): Flow<DomainResult<HostRule?, AppError>>
}

interface GetHostRuleByIdUseCase {
    /**
     * Gets a host rule by ID
     */
    suspend operator fun invoke(id: Long): DomainResult<HostRule?, AppError>
}

interface SaveHostRuleUseCase {
    /**
     * Creates or updates a host rule
     */
    suspend operator fun invoke(
        host: String, 
        status: UriStatus, 
        folderId: Long? = null,
        preferredBrowserPackage: String? = null,
        isPreferenceEnabled: Boolean = true
    ): DomainResult<Long, AppError>
}

interface DeleteHostRuleUseCase {
    /**
     * Deletes a host rule by ID
     */
    suspend operator fun invoke(id: Long): DomainResult<Unit, AppError>
}

interface GetAllHostRulesUseCase {
    /**
     * Gets all host rules
     */
    operator fun invoke(): Flow<DomainResult<List<HostRule>, AppError>>
}

interface GetHostRulesByStatusUseCase {
    /**
     * Gets host rules by status (bookmarked, blocked)
     */
    operator fun invoke(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>>
}

interface GetHostRulesByFolderUseCase {
    /**
     * Gets host rules in a specific folder
     */
    operator fun invoke(folderId: Long): Flow<DomainResult<List<HostRule>, AppError>>
}

interface GetRootHostRulesByStatusUseCase {
    /**
     * Gets host rules that are not in any folder, filtered by status
     */
    operator fun invoke(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>>
}

interface BookmarkHostUseCase {
    /**
     * Bookmarks a host and optionally assigns it to a folder
     */
    suspend operator fun invoke(host: String, folderId: Long? = null): DomainResult<Long, AppError>
}

interface BlockHostUseCase {
    /**
     * Blocks a host and optionally assigns it to a folder
     */
    suspend operator fun invoke(host: String, folderId: Long? = null): DomainResult<Long, AppError>
}

interface ClearHostStatusUseCase {
    /**
     * Clears bookmarked/blocked status for a host
     */
    suspend operator fun invoke(host: String): DomainResult<Unit, AppError>
}

interface UpdateHostRuleStatusUseCase {
    /**
     * Updates the status of a host rule and handles related folder/preference logic
     */
    suspend operator fun invoke(
        hostRuleId: Long,
        newStatus: UriStatus,
        folderId: Long? = null,
        preferredBrowserPackage: String? = null,
        isPreferenceEnabled: Boolean = true
    ): DomainResult<Unit, AppError>
}

interface CheckUriStatusUseCase {
    suspend operator fun invoke(host: String): Flow<DomainResult<UriStatus?, AppError>>
    // Explanation: This allows checking if a URI is bookmarked, blocked, or none, enforcing block precedence in the implementation.
}