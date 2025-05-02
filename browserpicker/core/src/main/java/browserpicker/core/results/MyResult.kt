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

/*
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


    suspend fun fold(
        onSuccess: suspend (value: T) -> Any,
        onFailure: suspend (exception: Throwable) -> Any
    ): T {
        return when (val exception = exceptionOrNull()) {
            null -> onSuccess(this as T)
            else -> onFailure(exception)
        } as T
    }
    fun onFailure(action: (exception: Throwable) -> Unit): Result<T> {
        exceptionOrNull()?.let { action(it) }
        return this as Result<T>
    }
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
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
    fun exceptionOrNull(): Throwable? = (this as? Error)?.error?.cause
    fun getOrThrow(): T {
        when (this) {
            is Success -> return data
            is Error -> throw error.cause?: RuntimeException("MyResult.Error without cause: ${error.message}")
        }
    }
}

 */

sealed interface MyResult<out T, out E: AppError> {
    data class Success<out T>(val data: T, val message: String? = null): MyResult<T, Nothing>
    data class Error<out E: AppError>(val error: E): MyResult<Nothing, E>

    suspend fun fold(
        onSuccess: suspend (value: T) -> Any,
        onFailure: suspend (exception: Throwable) -> Any,
    ): T {
        return when (val exception = exceptionOrNull()) {
            null -> onSuccess(this as T)
            else -> onFailure(exception)
        } as T
    }

    fun onFailure(action: (exception: Throwable) -> Unit): MyResult<T, E> {
        exceptionOrNull()?.let { action(it) }
        return this
    }

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
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

    fun exceptionOrNull(): Throwable? = (this as? Error)?.error?.cause
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw error.cause ?: RuntimeException("MyResult.Error without cause: ${error.message}")
        }
    }
}

fun <T> Flow<T>.asResult(
    unknownErrorMapper: (Throwable) -> AppError = { AppError.UnknownError("Flow collection failed", it) },
): Flow<MyResult<T, AppError>> {
    return this
        .map<T, MyResult<T, AppError>> { MyResult.Success(it) }
        .catch { throwable ->
            Timber.e(throwable.cause, "Flow<T>.asResult caught an exception: ${throwable.message}")
            emit(MyResult.Error(unknownErrorMapper(throwable)))
        }
}

fun <T, E: AppError> Flow<MyResult<T, E>>.safeCatch(
    errorMapper: (Throwable) -> E,
): Flow<MyResult<T, E>> {
    return this.catch { throwable ->
        Timber.e(throwable.cause, "Flow<MyResult<T, E>>.safeCatch caught an exception: ${throwable.message}")
        emit(MyResult.Error(errorMapper(throwable)))
    }
}
