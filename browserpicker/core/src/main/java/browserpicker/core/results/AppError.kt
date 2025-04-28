package browserpicker.core.results

sealed interface AppError {
    val message: String
    val cause: Throwable?

    data class UnknownError(override val message: String = "An unexpected error occurred.", override val cause: Throwable?): AppError
}

//sealed class DataSourceError(override val message: String, override val cause: Throwable?): AppError {
//    // TODO: Add more specific error types
//    class EmptyResolveInfo(override val cause: Throwable?): Exception("No apps handle ACTION_VIEW with http/https scheme", cause)
//    data class UnknownError(override val cause: Throwable?): DataSourceError("An unexpected error occurred.", cause)
//}
//
//sealed class RepositoryError(override val message: String, override val cause: Throwable?): AppError {
//    // TODO: Add more specific error types
//    data class UnknownError(override val cause: Throwable?): RepositoryError("An unexpected error occurred.", cause)
//}
//
//sealed class DomainError(override val message: String, override val cause: Throwable?): AppError {
//    // TODO: Add more specific error types
//    data class UnknownError(override val cause: Throwable?): DomainError("An unexpected error occurred.", cause)
//}
