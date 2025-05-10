package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCase
import javax.inject.Inject

class OpenBrowserPreferencesUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : OpenBrowserPreferencesUseCase {
    override suspend operator fun invoke(): DomainResult<Unit, AppError> {
        return try {
            systemRepository.openBrowserSettings()
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError(cause = e))
        }
    }
} 