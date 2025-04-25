package browserpicker.playground.browserpicker.core.common.utils

import browserpicker.playground.browserpicker.core.common.utils.getUserFriendlyMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed interface CustomError {
    val message: String
    val cause: Throwable?
    fun log() {
//        Timber.e(cause, message)
    }

    data class UnknownError(override val message: String, override val cause: Throwable?): CustomError
}

sealed interface DataSourceError: CustomError {
    data class UnknownError(override val message: String, override val cause: Throwable?): DataSourceError
}

sealed interface RepositoryError: CustomError {
    data class UnknownError(override val message: String, override val cause: Throwable?): RepositoryError
}

sealed interface DomainError: CustomError {
    data class UnknownError(override val message: String, override val cause: Throwable?): DomainError
}

// Helper function to create a user-readable description
fun CustomError.getUserFriendlyMessage(): String {
    return when (this) {
        is DataSourceError.UnknownError -> TODO()
        is DomainError.UnknownError -> TODO()
        is RepositoryError.UnknownError -> TODO()
        is CustomError.UnknownError -> TODO()
    }
}

sealed interface Result<out T, out E: CustomError> {
    data class Success<T>(val data: T) : Result<T, Nothing>
    data class Error<E : CustomError>(val error: E) : Result<Nothing, E>

    fun isSuccess(): Boolean = this is Success

    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = if (this is Success) data else null

    fun errorOrNull(): E? = if (this is Error) error else null
}
//sealed interface Result<out T, out E : AppError> {
//    data class Success<T>(val data: T) : Result<T, Nothing>
//    data class Error<E : AppError>(val error: E) : Result<Nothing, E>
//
//    fun isSuccess(): Boolean = this is Success
//
//    fun isError(): Boolean = this is Error
//
//    fun getOrNull(): T? = if (this is Success) data else null
//
//    fun errorOrNull(): E? = if (this is Error) error else null
//}

fun <T> Flow<T>.asResult(
    errorMapper: (Throwable) -> CustomError = { CustomError.UnknownError("Flow execution failed", it) }
): Flow<Result<T, CustomError>> {
    return this
        .map<T, Result<T, CustomError>> { Result.Success(it) }
        .catch { throwable ->
            emit(Result.Error(errorMapper(throwable)))
        }
}

fun <T, E : CustomError> Flow<Result<T, E>>.safeCatch(
    errorMapper: (Throwable) -> E
): Flow<Result<T, E>> {
    return this.catch { throwable ->
        emit(Result.Error(errorMapper(throwable)))
    }
}
