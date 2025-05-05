package com.dinesh.playground.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * A sealed class representing the outcome of a domain operation.
 * It can be either a [Success] containing data or a [Failure] containing an error.
 *
 * @param S The type of the success data.
 * @param F The type of the failure error. (We'll use a sealed Failure class for F)
 */
sealed class DomainResult<out S, out F> {
    data class Success<out S>(val data: S) : DomainResult<S, Nothing>()
    data class Failure<out F>(val error: F) : DomainResult<Nothing, F>()

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
    inline fun <R> mapFailure(transform: (F) -> R): DomainResult<S, R> {
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
        onFailure: (F) -> R
    ): R {
        return when (this) {
            is Success -> onSuccess(data)
            is Failure -> onFailure(error)
        }
    }
}

/**
 * A sealed class representing various types of domain-specific failures.
 * This provides a structured and extensible way to categorize errors.
 */
sealed class Failure {
    data class ValidationError(val errors: List<ValidationErrorDetail>) : Failure()
    data class ServerError(val message: String, val code: Int? = null) : Failure()
    data class NetworkError(val message: String) : Failure()
    data class NotFound(val message: String? = null) : Failure()
    data class Unauthorized(val message: String? = null) : Failure()
    data class BusinessRuleError(val message: String) : Failure() // For generic business logic failures
    data class DataMappingError(val message: String) : Failure() // For errors during DTO/Entity to Domain mapping
    data class Unknown(val throwable: Throwable? = null) : Failure() // For unexpected exceptions

    // Add more specific failure types as your application grows
    // Example: data class InsufficientPermissions(val permission: String) : Failure()
    // Example: data class FeatureDisabled(val featureName: String) : Failure()
}

// --- Extension Functions for Flow<DomainResult<S, Failure>> ---

/**
 * Transforms a Flow of DomainResult where the success data is a List<S>
 * into a Flow where the emitted lists are filtered based on a predicate,
 * while preserving failures.
 */
inline fun <S, F> Flow<DomainResult<List<S>, F>>.filterSuccessList(
    crossinline predicate: (S) -> Boolean
): Flow<DomainResult<List<S>, F>> {
    return this.map { result ->
        result.mapSuccess { list ->
            list.filter(predicate)
        }
    }
}

/**
 * Performs an action on the success data of each emitted [DomainResult] in the Flow.
 */
inline fun <S, F> Flow<DomainResult<S, F>>.onEachSuccess(
    crossinline action: suspend (S) -> Unit
): Flow<DomainResult<S, F>> {
    return this.map { result ->
        result.onSuccess { data -> action(data) }
        result
    }
}

/**
 * Performs an action on the failure error of each emitted [DomainResult] in the Flow.
 */
inline fun <S, F> Flow<DomainResult<S, F>>.onEachFailure(
    crossinline action: suspend (F) -> Unit
): Flow<DomainResult<S, F>> {
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
fun <S> Flow<DomainResult<S, Failure>>.catchUnexpected(): Flow<DomainResult<S, Failure>> {
    return this.catch { e ->
        // Only catch Throwables that haven't already been wrapped in a Failure
        if (e !is kotlinx.coroutines.CancellationException) { // Don't catch coroutine cancellation
            emit(DomainResult.Failure(Failure.Unknown(e)))
        } else {
            throw e // Re-throw CancellationException
        }
    }
}


// --- Example Usage in Repository ---

/*
// data/repository/UserRepositoryImpl.kt (using the new DomainResult)
package com.your_app.data.repository

import com.your_app.data.source.UserApiDataSource
import com.your_app.domain.model.User
import com.your_app.domain.common.DomainResult // Import the new Result type
import com.your_app.domain.common.Failure // Import the new Failure type
import com.your_app.domain.common.DataMappingError // Import specific failure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserRepositoryImpl(
    private val userApiDataSource: UserApiDataSource, // Dependency on DataSource interface
    // private val userDtoMapper: UserDtoMapper // Assuming you have a mapper object
) : UserRepository {

    // Returns a single Result for a list
    override suspend fun getUsers(): DomainResult<List<User>, Failure> {
        return try {
            val userDtos = userApiDataSource.getUsers()
            println("Repository: Received ${userDtos.size} DTOs from DataSource for getUsers")
            // Map DTOs to Domain Models - using manual mapping here, but a mapper object is better
            val users = userDtos.map { userDto ->
                 // Basic mapping validation/handling potential nulls
                 userDto.toDomainUser() // Assuming a mapper extension function or object call
            }
            println("Repository: Returning Success with ${users.size} Users for getUsers")
            DomainResult.Success(users)
        } catch (e: IOException) {
            println("Repository: Caught IOException for getUsers: ${e.message}")
            DomainResult.Failure(Failure.NetworkError(e.message ?: "Unknown network error"))
        } catch (e: Exception) {
             // Catch mapping errors or unexpected DataSource errors
             println("Repository: Caught other Exception for getUsers: ${e.message}")
             if (e is DataMappingException) { // Example custom exception from mapper
                  DomainResult.Failure(Failure.DataMappingError(e.message ?: "Data mapping failed"))
             } else {
                  DomainResult.Failure(Failure.ServerError("Could not fetch users: ${e.message}"))
             }
        }
    }

     // Implementation for Flow of lists return
    override fun getUsersFlow(): Flow<DomainResult<List<User>, Failure>> {
        return userApiDataSource.getUsersFlow() // Get the raw flow from DataSource
            .map { userDtos ->
                println("Repository: Received ${userDtos.size} DTOs from DataSource in Flow")
                // Map DTOs to Domain Models within the flow pipeline
                 // using manual mapping here, but a mapper object is better
                val users = userDtos.map { userDto ->
                    userDto.toDomainUser() // Assuming a mapper extension function or object call
                }
                 println("Repository: Emitting Success with ${users.size} Users in Flow")
                DomainResult.Success(users)
            }
            .catch { e ->
                // Catch exceptions emitted by the DataSource flow before they are mapped to DomainResult
                 println("Repository: Caught exception in Flow: ${e.message}")
                 // Emit a Failure result downstream instead of letting the exception crash the flow consumer
                when (e) {
                    is IOException -> emit(DomainResult.Failure(Failure.NetworkError(e.message ?: "Unknown network error")))
                    // is DataMappingException -> emit(DomainResult.Failure(Failure.DataMappingError(e.message ?: "Data mapping failed")))
                    else -> emit(DomainResult.Failure(Failure.ServerError("Could not fetch users stream: ${e.message}")))
                }
            }
            // .catchUnexpected() // Optional: Add this at the very end of the Flow processing chain
                                // if you want a final catch-all for unexpected runtime exceptions
                                // that weren't handled by the earlier catch.
    }
}

// Example of a simple mapping extension (better done in a dedicated Mapper class/object)
// Can live in data/mapper or domain/mapper depending on where you define the mapper
// For simplicity, let's put a basic one here for demonstration.
fun com.your_app.data.model.UserDto.toDomainUser(): User {
    // Basic mapping validation/handling potential nulls
    // If name is null, provide a default. If id is missing (shouldn't happen based on DTO),
    // you might throw a DataMappingException or return Failure.DataMappingError earlier.
    // This is where you'd ensure data integrity *during* mapping.
     if (this.name == null) {
          // Option 1: Map to default (if acceptable by domain)
         // return User(id = this.id, name = "Unnamed User", email = this.email ?: "")
         // Option 2: Throw an exception that the Repository's catch block will handle
         throw DataMappingException("UserDTO name is null, cannot map to non-nullable Domain User")
     }
    return User(
        id = this.id,
        name = this.name,
        email = this.email ?: "" // Assuming empty string is okay for null email
    )
}

// Example custom exception for mapping errors
class DataMappingException(message: String) : Exception(message)

*/
