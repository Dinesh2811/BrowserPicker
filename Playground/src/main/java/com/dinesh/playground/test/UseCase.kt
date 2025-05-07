//package com.dinesh.playground.test
//
//import kotlinx.coroutines.flow.*
//
//class GetUsersUseCase(
//    private val userRepository: UserRepository // Dependency
//) {
//
//    // Use Case for fetching a single list
//    suspend operator fun invoke(): Result<List<User>, Failure> {
//        println("UseCase: GetUsersUseCase started")
//        // 1. Fetch data from the Repository
//        return when (val result = userRepository.getUsers()) {
//            is Success -> {
//                val users = result.data
//                println("UseCase: Received Success with ${users.size} Users from Repository")
//
//                // --- BUSINESS RULE VALIDATION ---
//                val validationErrors = validateUsersBusinessRules(users)
//
//                if (validationErrors.isNotEmpty()) {
//                    println("UseCase: Validation failed, returning ValidationError")
//                    Failure.ValidationError(validationErrors)
//                }
//                // --- BUSINESS RULE REGARDING EMPTY LIST ---
//                // Example: If this *specific* use case requires at least one user
//                else if (users.isEmpty()) {
//                    println("UseCase: Received empty list, which is a failure for this use case. Returning NoDataFound")
//                    // Treat empty list as a specific business failure for *this use case*
//                    Failure.NoDataFound
//                }
//                // --- END OF BUSINESS RULES ---
//                else {
//                    println("UseCase: Validation and business rules passed, returning Success")
//                    Success(users) // Return success with valid data
//                }
//            }
//            is Failure -> {
//                println("UseCase: Received Failure from Repository: $result")
//                // If fetching from repository failed (network, server), pass the failure along
//                result
//            }
//        }
//    }
//
//    // Private function to perform business rule validation
//    private fun validateUsersBusinessRules(users: List<User>): List<ValidationErrorDetail> {
//        val errors = mutableListOf<ValidationErrorDetail>()
//        users.forEach { user ->
//            // Business Rule: User name must be at least 3 characters long
//            if (user.name.length < 3) {
//                errors.add(
//                    ValidationErrorDetail(
//                        field = "name",
//                        message = "User name must be at least 3 characters long for user ID ${user.id}"
//                    ))
//            }
//            // Add other business rule validations here
//            // e.g., if (!user.email.contains("@")) { ... }
//        }
//        return errors
//    }
//}
//
//
//class ObserveUsersUseCase(
//    private val userRepository: UserRepository // Dependency
//) {
//
//    // Use Case for observing a flow of lists
//    operator fun invoke(): Flow<Result<List<User>, Failure>> {
//        println("UseCase: ObserveUsersUseCase started")
//        // Get the Flow of Results from the Repository
//        return userRepository.getUsersFlow()
//            .map { result ->
//                // Process each emitted Result within the flow pipeline
//                when (result) {
//                    is Success -> {
//                        val users = result.data
//                        println("UseCase: Received Success with ${users.size} Users from Repository Flow")
//
//                        // --- BUSINESS RULE VALIDATION APPLIED PER EMISSION ---
//                        val validationErrors = validateUsersBusinessRules(users)
//
//                        if (validationErrors.isNotEmpty()) {
//                            println("UseCase: Validation failed for an emission, returning ValidationError")
//                            // If validation fails for a list emission, emit a ValidationError
//                            Failure.ValidationError(validationErrors) as Result<List<User>, Failure> // Cast needed
//                        }
//                        // --- BUSINESS RULE REGARDING EMPTY LIST PER EMISSION ---
//                        // Example: If THIS use case requires the observed list to *never* be empty
//                        else if (users.isEmpty()) {
//                            println("UseCase: Received empty list in Flow, which is a failure for this use case emission. Returning NoDataFound")
//                            // Treat empty list emission as a specific business failure
//                            Failure.NoDataFound as Result<List<User>, Failure> // Cast needed
//                        }
//                        // --- END OF BUSINESS RULES ---
//                        else {
//                            println("UseCase: Validation and business rules passed for emission, returning Success")
//                            // If validation and business rules pass, emit the successful result
//                            Success(users) as Result<List<User>, Failure> // Cast needed
//                        }
//                    }
//                    is Failure -> {
//                        println("UseCase: Received Failure from Repository Flow: $result")
//                        // If the Repository emitted a Failure, just pass it along
//                        result // Pass the failure downstream
//                    }
//                }
//            }
//        // No need for .catch here usually, as Repository's flow already caught exceptions
//        // and emitted them as Failure results within the flow.
//    }
//
//    // Private function to perform business rule validation (can be shared)
//    private fun validateUsersBusinessRules(users: List<User>): List<ValidationErrorDetail> {
//        val errors = mutableListOf<ValidationErrorDetail>()
//        users.forEach { user ->
//            // Business Rule: User name must be at least 3 characters long
//            if (user.name.length < 3) {
//                errors.add(
//                    ValidationErrorDetail(
//                        field = "name",
//                        message = "User name must be at least 3 characters long for user ID ${user.id}"
//                    )
//                )
//            }
//            // Add other business rule validations here
//        }
//        return errors
//    }
//}
