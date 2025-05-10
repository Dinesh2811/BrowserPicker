package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.OpenUriInBrowserUseCase
import javax.inject.Inject

class OpenUriInBrowserUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : OpenUriInBrowserUseCase {
    override suspend operator fun invoke(uriString: String, browserPackageName: String): DomainResult<Unit, AppError> {
        if (uriString.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("URI string cannot be blank."))
        }
        if (browserPackageName.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("Browser package name cannot be blank."))
        }
        return try {
            systemRepository.openUriInExternalBrowser(uriString, browserPackageName)
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError(cause = e))
        }
    }
} 