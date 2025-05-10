package browserpicker.domain.usecases.folder.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.usecases.folder.EnsureDefaultFoldersExistUseCase
import javax.inject.Inject

class EnsureDefaultFoldersExistUseCaseImpl @Inject constructor(
    private val folderRepository: FolderRepository
) : EnsureDefaultFoldersExistUseCase {
    override suspend operator fun invoke(): DomainResult<Unit, AppError> {
        return folderRepository.ensureDefaultFoldersExist()
    }
} 