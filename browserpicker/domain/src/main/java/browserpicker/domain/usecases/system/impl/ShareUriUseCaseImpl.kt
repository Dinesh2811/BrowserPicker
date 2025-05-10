package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.repository.SystemRepository
import browserpicker.domain.usecases.system.ShareUriUseCase
import javax.inject.Inject

class ShareUriUseCaseImpl @Inject constructor(
    private val systemRepository: SystemRepository
) : ShareUriUseCase {
    override suspend operator fun invoke(uriString: String): DomainResult<Unit, AppError> {
        if (uriString.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("URI string cannot be blank."))
        }
        return try {
            systemRepository.shareUri(uriString)
        } catch (e: Exception) {
            DomainResult.Failure(AppError.UnknownError(cause = e))
        }
    }
} 