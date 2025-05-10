package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import javax.inject.Inject

class SetAsDefaultBrowserUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : SetAsDefaultBrowserUseCase {
    override suspend operator fun invoke(): DomainResult<Boolean, AppError> {
        return try {
            systemRepository.requestSetDefaultBrowser()
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError(cause = e))
        }
    }
} 