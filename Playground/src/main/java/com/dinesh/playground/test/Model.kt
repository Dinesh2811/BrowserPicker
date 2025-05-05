package com.dinesh.playground.test

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlin.random.Random
import java.io.*

@Serializable
data class UserDto(
    val id: Int,
    val name: String?,
    val email: String?
    // Add other fields as per your API response
)


data class User(
    val id: Int,
    val name: String, // Assuming name is non-nullable after mapping
    val email: String
)

sealed class Result<out S, out F>

data class Success<out S>(val data: S) : Result<S, Nothing>()

sealed class Failure : Result<Nothing, Failure>() {
    data class ValidationError(val errors: List<ValidationErrorDetail>) : Failure()
    data class ServerError(val message: String) : Failure()
    data class NetworkError(val message: String) : Failure()
    data class UnknownError(val message: String) : Failure()
    // Add a specific failure type if an empty result IS a business failure in some use cases
    data object NoDataFound : Failure() // Example of a domain-specific "failure" for empty result
}

data class ValidationErrorDetail(
    val field: String?,
    val message: String
)

interface UserApiDataSource {
    // Returns a single list
    suspend fun getUsers(): List<UserDto>

    // Returns a flow of lists (e.g., from a database observing changes)
    fun getUsersFlow(): Flow<List<UserDto>>
}


class UserApiDataSourceImpl : UserApiDataSource {

    // Simulate fetching a single list (can be empty)
    override suspend fun getUsers(): List<UserDto> {
        delay(500) // Simulate network latency

        // --- Simulate different scenarios ---
        // throw IOException("Simulated Network Error in getUsers") // Simulate network failure
        // throw Exception("Simulated Unexpected Server Error in getUsers") // Simulate server-side error

        // Simulate sometimes returning an empty list
        val shouldReturnEmpty = Random.nextBoolean()
        if (shouldReturnEmpty) {
            println("DataSource: Returning empty list for getUsers")
            return emptyList()
        }

        // Simulate returning data (some will fail domain validation later)
        println("DataSource: Returning data for getUsers")
        return listOf(
            UserDto(id = 1, name = "Alice", email = "alice@example.com"),
            UserDto(id = 2, name = "Bob", email = "bob@example.com"),
            UserDto(id = 3, name = "C", email = "c@example.com"), // Name too short
            UserDto(id = 4, name = null, email = "dave@example.com"), // Null name
            UserDto(id = 5, name = "Eve", email = "eve@example.com")
        )
    }

    // Simulate fetching a flow of lists (e.g., from a database observer)
    override fun getUsersFlow(): Flow<List<UserDto>> = flow {
        println("DataSource: Flow started")
        // Simulate initial data load
        delay(300)
        emit(listOf(UserDto(id = 10, name = "FlowUser1", email = "flow1@example.com")))

        // Simulate updates over time
        delay(1000)
        emit(listOf(
            UserDto(id = 10, name = "FlowUser1Updated", email = "flow1@example.com"),
            UserDto(id = 11, name = "FlowUser2", email = "flow2@example.com")
        ))

        delay(1000)
        // Simulate an emission that will contain data failing domain validation
        emit(listOf(
            UserDto(id = 10, name = "FlowUser1Updated", email = "flow1@example.com"),
            UserDto(id = 11, name = "FlowUser2", email = "flow2@example.com"),
            UserDto(id = 12, name = "X", email = "shortname@example.com") // Name too short in Flow
        ))

        delay(1000)
        println("DataSource: Emitting empty list in Flow")
        emit(emptyList()) // Simulate emitting an empty list

        // Simulate an error later
        // delay(1000)
        // throw IOException("Simulated Network Error in getUsersFlow") // Simulate error
    }
}


interface UserRepository {
    // Returns a single Result for a list
    suspend fun getUsers(): Result<List<User>, Failure>

    // Returns a Flow of Results for a list stream
    fun getUsersFlow(): Flow<Result<List<User>, Failure>>
}


class UserRepositoryImpl(
    private val userApiDataSource: UserApiDataSource // Dependency on DataSource interface
) : UserRepository {

    // Implementation for single list return
    override suspend fun getUsers(): Result<List<User>, Failure> {
        return try {
            val userDtos = userApiDataSource.getUsers()
            println("Repository: Received ${userDtos.size} DTOs from DataSource for getUsers")
            // Map DTOs to Domain Models
            val users = userDtos.map { userDto ->
                // Basic mapping validation: handle potential nulls from DTO
                User(
                    id = userDto.id,
                    name = userDto.name ?: "Unnamed User", // Map null name to a default
                    email = userDto.email ?: ""
                )
            }
            // IMPORTANT: An empty list from DataSource is a Success at the Repository level
            println("Repository: Returning Success with ${users.size} Users for getUsers")
            Success(users)
        } catch (e: IOException) {
            println("Repository: Caught IOException for getUsers: ${e.message}")
            Failure.NetworkError(e.message ?: "Unknown network error")
        } catch (e: Exception) {
            println("Repository: Caught other Exception for getUsers: ${e.message}")
            Failure.ServerError("Could not fetch users: ${e.message}")
        }
    }

    // Implementation for Flow of lists return
    override fun getUsersFlow(): Flow<Result<List<User>, Failure>> {
        return userApiDataSource.getUsersFlow() // Get the raw flow from DataSource
            .map { userDtos ->
                println("Repository: Received ${userDtos.size} DTOs from DataSource in Flow")
                // Map DTOs to Domain Models within the flow pipeline
                val users = userDtos.map { userDto ->
                    // Basic mapping validation
                    User(
                        id = userDto.id,
                        name = userDto.name ?: "Unnamed User",
                        email = userDto.email ?: ""
                    )
                }
                // IMPORTANT: Each emitted empty list from DataSource is a Success emission in the Flow
                println("Repository: Emitting Success with ${users.size} Users in Flow")
                Success(users) as Result<List<User>, Failure> // Cast needed for sealed class variance
            }
            .catch { e ->
                // Catch exceptions emitted by the DataSource flow
                println("Repository: Caught exception in Flow: ${e.message}")
                // Emit a Failure result downstream instead of letting the exception crash the flow consumer
                when (e) {
                    is IOException -> emit(Failure.NetworkError(e.message ?: "Unknown network error"))
                    else -> emit(Failure.ServerError("Could not fetch users stream: ${e.message}"))
                }
            }
    }
}