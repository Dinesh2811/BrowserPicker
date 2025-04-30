package browserpicker.core.results

import androidx.compose.runtime.Immutable

sealed interface AppError {
    val message: String
    val cause: Throwable?
        get() = null

    data class Error(override val message: String = "An unexpected error occurred.", override val cause: Throwable?): AppError
}

@Immutable
sealed interface UriValidationError: AppError {
    data class BlankOrEmpty(override val message: String): UriValidationError
    data class Invalid(override val message: String, override val cause: Throwable? = null): UriValidationError
}

sealed class DataSourceError(override val message: String, override val cause: Throwable?): AppError {
    // TODO: Add more specific error types
    class EmptyResolveInfo(override val cause: Throwable?): Exception("No apps handle ACTION_VIEW with http/https scheme", cause)
    data class UnknownError(override val cause: Throwable?): DataSourceError("An unexpected error occurred.", cause)
}

sealed class RepositoryError(override val message: String, override val cause: Throwable?): AppError {
    // TODO: Add more specific error types
    data class UnknownError(override val cause: Throwable?): RepositoryError("An unexpected error occurred.", cause)
}

sealed class DomainError(override val message: String, override val cause: Throwable?): AppError {
    // TODO: Add more specific error types
    data class UnknownError(override val cause: Throwable?): DomainError("An unexpected error occurred.", cause)
}
