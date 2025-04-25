//package browserpicker.playground.browserpicker.core.common.utils
//
//import browserpicker.core.logDebug
//import browserpicker.core.logError
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.map
//
//sealed interface Result<out T, out E: AppError> {
//    data class Success<T>(val data: T) : Result<T, Nothing> {
//        init {
//            logDebug("Success called with data: $data")
//        }
//    }
//    data class Error<E: AppError>(val error: E): Result<Nothing, E> {
//        init {
//            logError("Error called with error: $error", error.cause)
//        }
//    }
//
//    fun isSuccess(): Boolean = this is Success
//
//    fun isError(): Boolean = this is Error
//
//    fun getOrNull(): T? = if (this is Success) data else null
//
//    fun errorOrNull(): E? = if (this is Error) error else null
//}
//
//fun <T> Flow<T>.asResult(
//    errorMapper: (Throwable) -> AppError = { AppError.UnknownError("Flow execution failed", it) }
//): Flow<Result<T, AppError>> {
//    return this
//        .map<T, Result<T, AppError>> { Result.Success(it) }
//        .catch { throwable ->
//            emit(Result.Error(errorMapper(throwable)))
//        }
//}
//
//fun <T, E : AppError> Flow<Result<T, E>>.safeCatch(
//    errorMapper: (Throwable) -> E
//): Flow<Result<T, E>> {
//    return this.catch { throwable ->
//        emit(Result.Error(errorMapper(throwable)))
//    }
//}
//
//fun AppError.getUserFriendlyMessage(): String {
//    return when (this) {
//        is DataSourceError.UnknownError -> TODO()
//        is DomainError.UnknownError -> TODO()
//        is RepositoryError.UnknownError -> TODO()
//        is AppError.UnknownError -> TODO()
//    }
//}