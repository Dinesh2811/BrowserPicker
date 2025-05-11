package browserpicker.presentation

import androidx.compose.runtime.Immutable
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    data class Success<out T>(val data: T) : UiState<T>

    @Immutable
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>

    /**
     * Properties to easily check the current state type
     */
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success<T>
    val isError: Boolean get() = this is Error

    /**
     * Returns the error details if the state is Error, otherwise null
     */
    fun errorOrNull(): Pair<String?, Throwable?> = (this as? Error)?.message to (this as? Error)?.cause
    val errorMessageOrNull: String? get() = errorOrNull().first

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

    /**
     * Folds the UiState into a single result by applying the provided functions
     * based on the current state.
     */
    fun <R> fold(
        onLoading: () -> R,
        onSuccess: (T) -> R,
        onError: (message: String, cause: Throwable?) -> R
    ): R = when (this) {
        is Loading -> onLoading()
        is Success -> onSuccess(data)
        is Error -> onError(message, cause)
    }
}

/**
 * Extension function to convert a Flow of DomainResult to a Flow of UiState
 */
fun <T> Flow<DomainResult<T, AppError>>.toUiState(): Flow<UiState<T>> {
    return this.map { result ->
        when (result) {
            is DomainResult.Success -> UiState.Success(result.data)
            is DomainResult.Failure -> UiState.Error(result.error.message, result.error.cause)
        }
    }
}

/**
 * Extension function to convert a DomainResult to a UiState
 */
fun <T> DomainResult<T, AppError>.toUiState(): UiState<T> {
    return when (this) {
        is DomainResult.Success -> UiState.Success(this.data)
        is DomainResult.Failure -> UiState.Error(this.error.message, this.error.cause)
    }
}

/**
 * Combines two Flows of UiState into a single Flow of UiState containing a Pair of their data.
 * The resulting state is Loading if either source is Loading.
 * The resulting state is Error if either source is Error.
 * The resulting state is Success only if both sources are Success.
 */
fun <T1, T2> Flow<UiState<T1>>.combineWith(
    other: Flow<UiState<T2>>
): Flow<UiState<Pair<T1, T2>>> {
    return this.combine(other) { state1, state2 ->
        when {
            state1 is UiState.Loading || state2 is UiState.Loading -> UiState.Loading
            state1 is UiState.Error -> state1
            state2 is UiState.Error -> state2
            state1 is UiState.Success && state2 is UiState.Success -> UiState.Success(Pair(state1.data, state2.data))
            else -> {
                // This case should theoretically not be reached if all states are covered,
                // but as a fallback or for unexpected states, we can represent it as an error.
                // Alternatively, you could throw an exception or use a specific 'Unknown' state.
                // For simplicity and robustness, let's treat it as an error.
                // A more specific error message might be helpful in a real application.
                UiState.Error("Unexpected state combination: $state1 and $state2")
            }
        }
    }
}

/**
 * Combines a list of Flows of UiState into a single Flow of UiState containing a List of their data.
 * The resulting state is Loading if any source is Loading.
 * The resulting state is Error if any source is Error (the first error encountered will be propagated).
 * The resulting state is Success only if all sources are Success.
 */
fun <T> List<Flow<UiState<T>>>.combineToListUiState(): Flow<UiState<List<T>>> {
    if (isEmpty()) return flowOf(UiState.Success(emptyList()))

    @Suppress("UNCHECKED_CAST") // Safe cast because combine transforms List<Flow<UiState<T>>> to Flow<List<UiState<T>>>
    return combine(this) { states: Array<UiState<T>> ->
        when {
            states.any { it.isLoading } -> UiState.Loading
            states.any { it.isError } -> {
                val firstError = states.first { it.isError } as UiState.Error
                UiState.Error(firstError.message, firstError.cause)
            }
            states.all { it.isSuccess } -> {
                val dataList = states.map { (it as UiState.Success).data }
                UiState.Success(dataList)
            }
            else -> {
                // This case should ideally not be reached
                UiState.Error("Unexpected state combination in combineToListUiState")
            }
        }
    }
}
