package browserpicker.domain.usecases.system.impl

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.UriSource
import browserpicker.domain.usecases.system.HandleUncaughtUriUseCase
import browserpicker.domain.usecases.uri.shared.HandleUriUseCase
import browserpicker.domain.usecases.uri.shared.ValidateUriUseCase
import javax.inject.Inject

class HandleUncaughtUriUseCaseImpl @Inject constructor(
    private val validateUriUseCase: ValidateUriUseCase,
    private val handleUriUseCase: HandleUriUseCase
) : HandleUncaughtUriUseCase {
    override suspend operator fun invoke(data: String): DomainResult<Boolean, AppError> {
        if (data.isBlank()) {
            return DomainResult.Failure(AppError.ValidationError("URI data cannot be blank."))
        }

        val uriString = data

        // Validate the URI first
        val validationResult = validateUriUseCase(uriString)

        return when (validationResult) {
            is DomainResult.Success -> {
                val parsedUri = validationResult.data
                if (parsedUri == null) {
                    // Not a valid web URI according to ValidateUriUseCase
                    DomainResult.Success(false) // Or a specific error if validation indicates invalid format
                } else {
                    // It's a valid URI, now try to handle it.
                    // Source is unknown or needs to be determined, using MANUAL as a placeholder.
                    // The actual source might depend on how this 'uncaught' URI was obtained.
                    handleUriUseCase(uriString, UriSource.MANUAL) // Call suspend function directly
                        .mapSuccess { handleResult ->
                            // Successfully initiated handling, return true.
                            // The actual outcome of handling (blocked, opened, picker shown) is in handleResult.
                            true
                        }
                }
            }
            is DomainResult.Failure -> {
                // Validation failed, propagate the error
                DomainResult.Failure(validationResult.error)
            }
        }
    }
}
