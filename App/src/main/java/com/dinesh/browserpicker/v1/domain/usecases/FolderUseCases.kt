package com.dinesh.browserpicker.v1.domain.usecases

import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import kotlinx.coroutines.flow.Flow

interface GetFolderUseCase {
    /**
     * Gets a folder by ID
     */
    operator fun invoke(folderId: Long): Flow<DomainResult<Folder?, AppError>>
}

interface GetChildFoldersUseCase {
    /**
     * Gets all child folders of the specified parent folder
     */
    operator fun invoke(parentFolderId: Long): Flow<DomainResult<List<Folder>, AppError>>
}

interface GetRootFoldersUseCase {
    /**
     * Gets all root folders of the specified type
     */
    operator fun invoke(type: FolderType): Flow<DomainResult<List<Folder>, AppError>>
}

interface GetAllFoldersByTypeUseCase {
    /**
     * Gets all folders of the specified type (including nested)
     */
    operator fun invoke(type: FolderType): Flow<DomainResult<List<Folder>, AppError>>
}

interface FindFolderByNameAndParentUseCase {
    /**
     * Finds a folder by name and parent folder ID
     */
    suspend operator fun invoke(name: String, parentFolderId: Long?, type: FolderType): DomainResult<Folder?, AppError>
}

interface CreateFolderUseCase {
    /**
     * Creates a new folder
     */
    suspend operator fun invoke(name: String, parentFolderId: Long?, type: FolderType): DomainResult<Long, AppError>
}

interface UpdateFolderUseCase {
    /**
     * Updates an existing folder
     */
    suspend operator fun invoke(folder: Folder): DomainResult<Unit, AppError>
}

interface DeleteFolderUseCase {
    /**
     * Deletes a folder if it's empty or cascade deletes its contents if requested
     */
    suspend operator fun invoke(folderId: Long, forceCascade: Boolean = false): DomainResult<Unit, AppError>
}

interface MoveFolderUseCase {
    /**
     * Moves a folder to a new parent folder
     */
    suspend operator fun invoke(folderId: Long, newParentFolderId: Long?): DomainResult<Unit, AppError>
}

interface MoveHostRuleToFolderUseCase {
    /**
     * Moves a host rule to a different folder
     */
    suspend operator fun invoke(hostRuleId: Long, destinationFolderId: Long?): DomainResult<Unit, AppError>
}

interface GetFolderHierarchyUseCase {
    /**
     * Gets the full folder path (parent hierarchy) for a specified folder
     */
    suspend operator fun invoke(folderId: Long): DomainResult<List<Folder>, AppError>
}

interface EnsureDefaultFoldersExistUseCase {
    /**
     * Ensures that the default bookmark and block root folders exist
     */
    suspend operator fun invoke(): DomainResult<Unit, AppError>
} 