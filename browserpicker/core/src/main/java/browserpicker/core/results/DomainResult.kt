package browserpicker.core.results

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.Result as KotlinResult

@Immutable
sealed interface UriValidationError: AppError {
    data class BlankOrEmpty(override val message: String): UriValidationError
    data class Invalid(override val message: String, override val cause: Throwable? = null): UriValidationError
}

sealed interface AppError {
    val message: String
    val cause: Throwable?
        get() = null

    data class UnknownError(override val message: String = "An unexpected error occurred.", override val cause: Throwable? = null): AppError
    data class DataNotFound(override val message: String, override val cause: Throwable? = null): AppError
    data class DataIntegrityError(override val message: String, override val cause: Throwable? = null): AppError
    data class FolderNotEmptyError(val folderId: Long, override val message: String, override val cause: Throwable? = null): AppError
    data class DatabaseError(override val message: String, override val cause: Throwable? = null): AppError
    data class DataMappingError(override val message: String): AppError
    data class ValidationError(override val message: String): AppError
//    data class ValidationError(val errors: List<Pair<String, String>>, override val message: String = errors.joinToString { (field, message) -> message }): AppError
//    data class ServerError(override val message: String, val code: Int? = null): Failure()
//    data class NetworkError(override val message: String): Failure()
//    data class NotFound(override val message: String): Failure()
//    data class Unauthorized(override val message: String): Failure()
//    data class BusinessRuleError(override val message: String): Failure()
//    data class DataMappingError(override val message: String): Failure()
//    data class UnknownError(val throwable: Throwable? = null, override val message: String = "An unexpected error occurred." + (throwable?.message?.let { ": $it" })): AppError
}

/**
 * A sealed class representing the outcome of a domain operation.
 * It can be either a [Success] containing data or a [Failure] containing an error.
 *
 * @param S The type of the success data.
 * @param F The type of the failure error. (We'll use a sealed Failure class for F)
 */
sealed class DomainResult<out S, out F: AppError> {
    data class Success<out S>(val data: S): DomainResult<S, Nothing>()
    data class Failure<out F: AppError>(val error: F): DomainResult<Nothing, F>()

    /**
     * Returns the success data if the result is [Success], otherwise returns null.
     */
    fun getOrNull(): S? = when (this) {
        is Success -> data
        is Failure -> null
    }

    /**
     * Returns the failure error if the result is [Failure], otherwise returns null.
     */
    fun errorOrNull(): F? = when (this) {
        is Success -> null
        is Failure -> error
    }

    /**
     * Returns true if the result is [Success].
     */
    val isSuccess: Boolean get() = this is Success<S>

    /**
     * Returns true if the result is [Failure].
     */
    val isFailure: Boolean get() = this is Failure<F>

    fun exceptionOrNull(): Throwable? = (this as? Failure)?.error?.cause

    /**
     * Transforms the success data using the provided [transform] function.
     * If the result is [Failure], the original failure is preserved.
     */
    inline fun <R> mapSuccess(transform: (S) -> R): DomainResult<R, F> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> Failure(error)
        }
    }

    /**
     * Transforms the failure error using the provided [transform] function.
     * If the result is [Success], the original success is preserved.
     * This is useful for mapping lower-level failures to higher-level ones.
     */
    inline fun <R: AppError> mapFailure(transform: (F) -> R): DomainResult<S, R> {
        return when (this) {
            is Success -> Success(data)
            is Failure -> Failure(transform(error))
        }
    }

    /**
     * Performs an action on the success data if the result is [Success].
     */
    inline fun onSuccess(action: (S) -> Unit): DomainResult<S, F> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Performs an action on the failure error if the result is [Failure].
     */
    inline fun onFailure(action: (F) -> Unit): DomainResult<S, F> {
        if (this is Failure) {
            action(error)
        }
        return this
    }

    /**
     * Handles both success and failure cases by applying the appropriate function.
     * Provides a convenient way to process the result without extensive 'when' statements.
     */
    inline fun <R> fold(
        onSuccess: (S) -> R,
        onFailure: (F) -> R,
    ): R {
        return when (this) {
            is Success -> onSuccess(data)
            is Failure -> onFailure(error)
        }
    }


    /**
     * Returns the success data if the result is [Success], otherwise returns the result
     * of the [onFailure] function applied to the error.
     */
    inline fun getOrElse(onFailure: (F) -> @UnsafeVariance S): S {
        return when (this) {
            is Success -> data
            is Failure -> onFailure(error)
        }
    }

    /**
     * Returns the success data if the result is [Success], otherwise throws the result
     * of the [exceptionMapper] function applied to the error.
     */
    inline fun getOrThrow(exceptionMapper: (F) -> Throwable): S {
        return when (this) {
            is Success -> data
            is Failure -> throw exceptionMapper(error)
        }
    }

    /**
     * Recovers from a failure by applying the [recoveryFunction] to the error.
     * If the result is [Success], the original success is preserved.
     * Useful for attempting alternative operations on failure.
     */
    inline fun recover(recoveryFunction: (F) -> DomainResult<@UnsafeVariance S, @UnsafeVariance F>): DomainResult<S, F> {
        return when (this) {
            is Success -> this
            is Failure -> recoveryFunction(error)
        }
    }

    /**
     * Chains another operation that returns a DomainResult, applied to the success data.
     * If the current result is Failure, the failure is propagated.
     * Similar to flatMap in other contexts.
     */
    inline fun <R> andThen(
        crossinline nextOperation: (S) -> DomainResult<R, @UnsafeVariance F>
    ): DomainResult<R, F> {
        return when (this) {
            is Success -> nextOperation(data)
            is Failure -> DomainResult.Failure(error)
        }
    }
}


/**
 * A sealed class representing various types of domain-specific failures.
 * This provides a structured and extensible way to categorize errors.
 */
//sealed class Failure: AppError {
//    data class ValidationError(val errors: List<Pair<String, String>>, override val message: String = errors.joinToString { (field, message) -> message }): Failure()
//    data class ServerError(override val message: String, val code: Int? = null): Failure()
//    data class NetworkError(override val message: String): Failure()
//    data class NotFound(override val message: String): Failure()
//    data class Unauthorized(override val message: String): Failure()
//    data class BusinessRuleError(override val message: String): Failure()
//    data class DataMappingError(override val message: String): Failure()
//    data class Unknown(val throwable: Throwable? = null, override val message: String = "Unknown error" + (throwable?.message?.let { ": $it" })): Failure()
//}

/**
 * Transforms a Flow of DomainResult where the success data is a List<S>
 * into a Flow where the emitted lists are filtered based on a predicate,
 * while preserving failures.
 */
inline fun <S> Flow<DomainResult<List<S>, AppError>>.filterSuccessList(
    crossinline predicate: (S) -> Boolean,
): Flow<DomainResult<List<S>, AppError>> {
    return this.map { result ->
        result.mapSuccess { list ->
            list.filter(predicate)
        }
    }
}

/**
 * Performs an action on the success data of each emitted [DomainResult] in the Flow.
 */
inline fun <S> Flow<DomainResult<S, AppError>>.onEachSuccess(
    crossinline action: suspend (S) -> Unit,
): Flow<DomainResult<S, AppError>> {
    return this.map { result ->
        result.onSuccess { data -> action(data) }
        result
    }
}

/**
 * Performs an action on the failure error of each emitted [DomainResult] in the Flow.
 */
inline fun <S> Flow<DomainResult<S, AppError>>.onEachFailure(
    crossinline action: suspend (AppError) -> Unit,
): Flow<DomainResult<S, AppError>> {
    return this.map { result ->
        result.onFailure { error -> action(error) }
        result
    }
}

/**
 * Catches exceptions in the upstream Flow and emits a [DomainResult.Failure] with an [Failure.Unknown].
 * Use this as a last resort catch-all for unexpected errors in a flow pipeline.
 * Repository flows should often use .catch { emit(Failure(...)) } before mapping to DomainResult.
 */
fun <S> Flow<DomainResult<S, AppError>>.catchUnexpected(): Flow<DomainResult<S, AppError>> {
    return this.catch { e ->
        if (e !is kotlinx.coroutines.CancellationException) {
            emit(DomainResult.Failure(AppError.UnknownError(cause = e)))
        } else {
            throw e
        }
    }
}

inline fun <S> KotlinResult<S>.toDomainResult(
    failureMapper: (Throwable) -> AppError,
): DomainResult<S, AppError> {
    return fold(
        onSuccess = { data -> DomainResult.Success(data) },
        onFailure = { throwable -> DomainResult.Failure(failureMapper(throwable)) }
    )
}

/**
 * Transforms a DomainResult with a List<S> success into a DomainResult with a List<R> success
 * by mapping each item in the list, preserving failures.
 */
inline fun <S, R, F: AppError> DomainResult<List<S>, F>.mapSuccessList(
    transform: (S) -> R
): DomainResult<List<R>, F> {
    return this.mapSuccess { list ->
        list.map(transform)
    }
}

/**
 * Transforms a DomainResult with a List<S> success into a DomainResult with a List<S> success
 * by filtering the list based on a predicate, preserving failures.
 */
inline fun <S, F: AppError> DomainResult<List<S>, F>.filterSuccessList(
    predicate: (S) -> Boolean
): DomainResult<List<S>, F> {
    return this.mapSuccess { list ->
        list.filter(predicate)
    }
}
