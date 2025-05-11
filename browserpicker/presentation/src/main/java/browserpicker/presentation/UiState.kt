package browserpicker.presentation

import androidx.compose.runtime.Immutable
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Sealed interface that represents UI states for the presentation layer.
 * This can be used consistently across all ViewModels to represent loading, success, and error states.
 */
@Immutable
sealed interface UiState<out T> {
    @Immutable
    data object Loading : UiState<Nothing>

    @Immutable
    data class Success<T>(val data: T) : UiState<T>

    @Immutable
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>

    /**
     * Maps the success data to another type using the provided transform function
     */
    fun <R> map(transform: (T) -> R): UiState<R> = when (this) {
        is Loading -> Loading
        is Error -> this
        is Success -> Success(transform(data))
    }

    /**
     * Applies the provided action if this is a success state
     */
    fun onSuccess(action: (T) -> Unit): UiState<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Applies the provided action if this is an error state
     */
    fun onError(action: (message: String, cause: Throwable?) -> Unit): UiState<T> {
        if (this is Error) action(message, cause)
        return this
    }

    /**
     * Applies the provided action if this is a loading state
     */
    fun onLoading(action: () -> Unit): UiState<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * Returns the success data or null if this is not a success state
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the success data or the result of the provided function if this is not a success state
     */
    fun getOrElse(defaultValue: () -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue()
    }
}

/**
 * Extension function to convert a Flow of DomainResult to a Flow of UiState
 */
fun <T> Flow<DomainResult<T, AppError>>.toUiState(): Flow<UiState<T>> {
    return this.map { result ->
        when (result) {
            is DomainResult.Success -> UiState.Success(result.data)
            is DomainResult.Failure -> UiState.Error(
                result.error.message,
                result.error.cause
            )
        }
    }
}

/**
 * Extension function to convert a DomainResult to a UiState
 */
fun <T> DomainResult<T, AppError>.toUiState(): UiState<T> {
    return when (this) {
        is DomainResult.Success -> UiState.Success(this.data)
        is DomainResult.Failure -> UiState.Error(
            this.error.message,
            this.error.cause
        )
    }
}
