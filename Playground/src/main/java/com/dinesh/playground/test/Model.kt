package com.dinesh.playground.test

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlin.random.Random
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

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

//data class Success<out S>(val data: S) : Result<S, Nothing>()


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
    suspend fun getUsers(): DomainResult<List<User>, Failure>

    // Returns a Flow of Results for a list stream
    fun getUsersFlow(): Flow<DomainResult<List<User>, Failure>>
}


// Annotate RepositoryImpl constructor with @Inject and inject the mapper
@Singleton // Repository is often a singleton
class UserRepositoryImpl @Inject constructor(
    private val userApiDataSource: UserApiDataSource, // Injected DataSource
    private val userMapper: UserMapper // Injected Mapper (using interface)
) : UserRepository {

    // Example using try-catch in Repository
    override suspend fun getUsers(): DomainResult<List<User>, Failure> {
        return try {
            val userDtos = userApiDataSource.getUsers()
            val users = userMapper.mapDtoListToDomainList(userDtos) // Mapping can also throw

            DomainResult.Success(users)

        } catch (e: IOException) {
            // Explicitly catch network errors
            DomainResult.Failure(Failure.NetworkError(e.message ?: "Network error"))
//        } catch (e: DataMappingException) {
//            // Explicitly catch mapping errors
//            DomainResult.Failure(Failure.DataMappingError(e.message ?: "Data mapping failed"))
        } catch (e: Exception) {
            // Catch any other unexpected exceptions
            DomainResult.Failure(Failure.Unknown(e))
        }
    }
    
    // Example using runCatching in Repository
     suspend fun getUsers1(): DomainResult<List<User>, Failure> {
        return runCatching {
            // Code that might throw exceptions goes here
            val userDtos = userApiDataSource.getUsers()
            val users = userMapper.mapDtoListToDomainList(userDtos)
            users // The result of the success path
        }.fold(
            onSuccess = { users ->
                // Map kotlin.Result.Success to DomainResult.Success
                DomainResult.Success(users)
            },
            onFailure = { throwable ->
                // Map kotlin.Result.Failure (Throwable) to DomainResult.Failure (Failure subtype)
                when (throwable) {
                    is IOException -> Failure.NetworkError(throwable.message ?: "Network error")
//                    is DataMappingException -> Failure.DataMappingError(throwable.message ?: "Data mapping failed")
                    else -> Failure.Unknown(throwable)
                }.let { failure ->
                    DomainResult.Failure(failure) // Wrap the mapped failure in DomainResult.Failure
                }
            }
        )
    }


    suspend fun getUsers2(): DomainResult<List<User>, Failure> {
        return runCatching {
            val userDtos = userApiDataSource.getUsers()
            userMapper.mapDtoListToDomainList(userDtos) // This is what goes into runCatching
        }.toDomainResult { throwable -> // Use the new extension function
            // Provide the mapping from Throwable to your Failure subtypes
            when (throwable) {
                is IOException -> Failure.NetworkError(throwable.message ?: "Network error")
//                is DataMappingException -> Failure.DataMappingError(throwable.message ?: "Data mapping failed")
                else -> Failure.Unknown(throwable)
            }
        }
    }

    // Implementation for Flow of lists return
    override fun getUsersFlow(): Flow<DomainResult<List<User>, Failure>> {
        return userApiDataSource.getUsersFlow() // Get the raw flow from DataSource
            .map { userDtos ->
                println("Repository: Received ${userDtos.size} DTOs from DataSource in Flow")
                // --- Use the injected mapper to convert the list within the flow pipeline ---
                val users = userMapper.mapDtoListToDomainList(userDtos)
                // ------------------------------------------------------------------------
                println("Repository: Emitting Success with ${users.size} Users in Flow")
                DomainResult.Success(users) as DomainResult<List<User>, Failure> // Cast needed
            }
            .catch { e ->
                // Catch exceptions from the DataSource flow or potential mapping exceptions
                println("Repository: Caught exception in Flow: ${e.message}")
                // Emit a Failure result downstream
                when (e) {
                    is IOException -> emit(DomainResult.Failure(Failure.NetworkError(e.message ?: "Unknown network error")))
                    // if (e is CustomMappingException) {
                    //     emit(DomainResult.Failure(Failure.DataMappingError(e.message ?: "Mapping failed in flow")))
                    // } else {
                    else -> emit(DomainResult.Failure(Failure.ServerError("Could not fetch users stream: ${e.message}")))
                    // }
                }
            }
    }
}


// Optional: Define an interface for better testability
interface UserMapper {
    fun mapDtoToDomain(dto: UserDto): User
    fun mapDtoListToDomainList(dtos: List<UserDto>): List<User>
    // Add mapping from Entity to Domain if you have a database source
    // fun mapEntityToDomain(entity: UserEntity): User
    // fun mapEntityListToDomainList(entities: List<UserEntity>): List<User>
}


// Annotate the constructor with @Inject to allow Hilt to create instances
@Singleton // Annotate with a scope if it's stateless and reusable
class UserDtoMapperImpl @Inject constructor() : UserMapper { // Implement the interface

    // Implement the mapping logic
    override fun mapDtoToDomain(dto: UserDto): User {
        // Basic mapping validation/handling potential nulls from DTO
        // If mapping fails critically (e.g., required non-nullable field is null),
        // you might throw a custom DataMappingException here that the Repository catches.
        // For simplicity, handling null with a default here.
        if (dto.name == null) {
            // Depending on strictness, you might throw or map to a default/error state earlier
            // For this example, we'll assume mapping null name to "Unnamed User" is acceptable here.
            // In a stricter scenario, the Repository's catch block would handle an exception thrown here.
        }

        return User(
            id = dto.id,
            name = dto.name ?: "Unnamed User", // Handle potential null name from DTO
            email = dto.email ?: "" // Handle potential null email from DTO
        )
    }

    // Implement list mapping (often just maps each item)
    override fun mapDtoListToDomainList(dtos: List<UserDto>): List<User> {
        return dtos.map { mapDtoToDomain(it) }
    }

    // Implement other mapping methods as needed (e.g., Entity to Domain)
}
