package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckDefaultBrowserStatusUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : CheckDefaultBrowserStatusUseCase {
    override operator fun invoke(): Flow<DomainResult<Boolean, AppError>> {
        return systemRepository.isDefaultBrowser()
            .catchUnexpected() // Ensures any unexpected exception is mapped to AppError.UnknownError
    }
} 