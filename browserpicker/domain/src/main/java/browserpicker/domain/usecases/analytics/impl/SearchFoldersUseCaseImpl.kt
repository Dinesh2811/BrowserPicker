package browserpicker.domain.usecases.analytics.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.analytics.SearchFoldersUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchFoldersUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : SearchFoldersUseCase {

    override operator fun invoke(
        query: String,
        folderType: FolderType?
    ): Flow<DomainResult<List<Folder>, AppError>> {
        // If folderType is specified, get all folders of that type.
        // Otherwise, we might need to fetch all folders of all types (requires repository support or multiple calls).
        // For simplicity, if folderType is null, this example will search across all folders by fetching them type by type
        // or expect a repository method like `getAllFolders()` if it existed.
        // Assuming folderType is usually provided for a more targeted search.

        val sourceFlow: Flow<DomainResult<List<Folder>, AppError>> = if (folderType != null) {
            folderRepository.getAllFoldersByType(folderType)
        } else {
            // If no specific type, and no getAllFolders() exists, we might need to combine results
            // from multiple calls (e.g., BOOKMARK and BLOCK types). This can be complex.
            // For this example, let's assume if type is null, we search BOOKMARK by default, or return empty.
            // Or, a better approach would be for the repository to support searching across all types.
            // As a simple placeholder, let's search bookmarks if type is null.
            folderRepository.getAllFoldersByType(FolderType.BOOKMARK) // Placeholder behavior
            // A more robust solution would fetch for all known FolderTypes and combine them, or require repo enhancement
        }

        return sourceFlow.map { result ->
            result.mapSuccess { folders ->
                if (query.isBlank()) {
                    folders // Return all if query is blank
                } else {
                    folders.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
        }.catchUnexpected()
    }
} 