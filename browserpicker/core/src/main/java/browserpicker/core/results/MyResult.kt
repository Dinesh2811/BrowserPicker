package browserpicker.core.results

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

//fun <T, E: AppError>  MyResult<T, AppError>.throwOnFailure() {
////    if (this is MyResult.Error) throw this.error.cause!!
//    if (this is MyResult.Error) throw this.error.cause!!
//}
//fun <T, E: AppError> MyResult<T, AppError>.getOrThrow(): AppError {
//    this.throwOnFailure<T, AppError>()
//    return this as AppError
//}

sealed interface MyResult<out T, out E : AppError> {
    data class Success<out T>(val data: T, val message: String? = null) : MyResult<T, Nothing> {
        init {
            Timber.d("MyResult.Success: $data")
        }
    }

    data class Error<out E : AppError>(val error: E) : MyResult<Nothing, E> {
        init {
            Timber.e(error.cause, "MyResult.Error: ${error.message}")
        }
    }


//    fun <T, E: AppError> exceptionOrNull(): Throwable? = (this as? Error)?.error?.cause
//    fun <T, E: AppError> exceptionOrNull(): Throwable? {
//        return when {
//            this is Throwable -> this.cause
//            else -> null
//        }
//    }


    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): E? = (this as? Error)?.error
    fun onSuccess(action: (T) -> Unit): MyResult<T, E> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    fun onError(action: (E) -> Unit): MyResult<T, E> {
        if (this is Error) {
            action(error)
        }
        return this
    }
    fun <R> map(transform: (T) -> R): MyResult<R, E> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    fun <T, E: AppError> exceptionOrNull(): Throwable? = (this as? Error)?.error?.cause
    fun <T, E: AppError> throwOnFailure() = exceptionOrNull<T, AppError>()?: Exception("Unknown error")
    fun <T, E: AppError> getOrThrow(): AppError {
        this.throwOnFailure<T, AppError>()
        return this as AppError
    }
}

fun <T> Flow<T>.asResult(
    errorMapper: (Throwable) -> AppError = { AppError.Error("Flow collection failed", it) },
): Flow<MyResult<T, AppError>> {
    return this
        .map<T, MyResult<T, AppError>> { MyResult.Success(it) }
        .catch { throwable ->
            Timber.e(throwable.cause, "Flow<T>.asResult caught an exception: ${throwable.message}")
            emit(MyResult.Error(errorMapper(throwable)))
        }
}

fun <T, E : AppError> Flow<MyResult<T, E>>.safeCatch(
    errorMapper: (Throwable) -> E,
): Flow<MyResult<T, E>> {
    return this.catch { throwable ->
        Timber.e(throwable.cause, "Flow<MyResult<T, E>>.safeCatch caught an exception: ${throwable.message}")
        emit(MyResult.Error(errorMapper(throwable)))
    }
}
