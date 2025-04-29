package browserpicker.domain.service

sealed interface DomainError {
    data class Validation(val message: String): DomainError
    data class NotFound(val entityType: String, val identifier: String): DomainError
    data class Conflict(val message: String): DomainError
    data class Database(val underlyingCause: Throwable?): DomainError
    data class Unexpected(val message: String, val cause: Throwable? = null): DomainError
    data class Custom(val message: String): DomainError
}

fun Throwable.toDomainError(defaultMessage: String = "An unexpected error occurred"): DomainError {
    return when (this) {
        is IllegalArgumentException -> DomainError.Validation(this.message ?: "Invalid input.")
        is IllegalStateException -> DomainError.Conflict(this.message ?: "Operation conflict.")
        else -> DomainError.Unexpected(this.message ?: defaultMessage, this)
    }
}
