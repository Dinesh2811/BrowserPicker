//package browserpicker.core.results
//
//import browserpicker.core.utils.logDebug
//import browserpicker.core.utils.logError
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.map
//
//sealed interface Result<out T, out E : AppError> {
//    data class Success<out T>(val data: T) : Result<T, Nothing> {
//        init {
//            logDebug("Result.Success: $data")
//        }
//    }
//
//    data class Error<out E : AppError>(val error: E) : Result<Nothing, E> {
//        init {
//            logError("Result.Error: ${error.message}", error.cause)
//        }
//    }
//
//    fun isSuccess(): Boolean = this is Success
//    fun isError(): Boolean = this is Error
//    fun getOrNull(): T? = (this as? Success)?.data
//    fun errorOrNull(): E? = (this as? Error)?.error
//    fun onSuccess(action: (T) -> Unit): Result<T, E> {
//        if (this is Success) {
//            action(data)
//        }
//        return this
//    }
//    fun onError(action: (E) -> Unit): Result<T, E> {
//        if (this is Error) {
//            action(error)
//        }
//        return this
//    }
//    fun <R> map(transform: (T) -> R): Result<R, E> {
//        return when (this) {
//            is Success -> Success(transform(data))
//            is Error -> this
//        }
//    }
//}
//
//fun <T> Flow<T>.asResult(
//    errorMapper: (Throwable) -> AppError = { AppError.UnknownError("Flow collection failed", it) }
//): Flow<Result<T, AppError>> {
//    return this
//        .map<T, Result<T, AppError>> { Result.Success(it) }
//        .catch { throwable ->
//            logError("Flow<T>.asResult caught an exception", throwable)
//            emit(Result.Error(errorMapper(throwable)))
//        }
//}
//
//fun <T, E : AppError> Flow<Result<T, E>>.safeCatch(
//    errorMapper: (Throwable) -> E
//): Flow<Result<T, E>> {
//    return this.catch { throwable ->
//        logError("Flow<Result<T, E>>.safeCatch caught an exception", throwable)
//        emit(Result.Error(errorMapper(throwable)))
//    }
//}
