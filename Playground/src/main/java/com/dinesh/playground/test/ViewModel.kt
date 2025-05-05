package com.dinesh.playground.test


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn // Import launchIn
import kotlinx.coroutines.flow.onEach // Import onEach
import kotlinx.coroutines.launch
import java.io.IOException

// Example ViewModel consuming the Use Cases
class UserViewModel /*@Inject constructor(...)*/ : ViewModel() {

    // Manual dependencies for demonstration
    private val userDataSource = UserApiDataSourceImpl()
    private val userRepository = UserRepositoryImpl(userDataSource)
    private val getUsersUseCase = GetUsersUseCase(userRepository) // Single list use case
    private val observeUsersUseCase = ObserveUsersUseCase(userRepository) // Flow use case

    // State for the single list Use Case
    private val _singleUserState = MutableStateFlow<UserState>(UserState.Idle) // Use Idle initially
    val singleUserState: StateFlow<UserState> = _singleUserState

    // State for the Flow Use Case
    private val _flowUserState = MutableStateFlow<UserState>(UserState.Idle) // Use Idle initially
    val flowUserState: StateFlow<UserState> = _flowUserState

    // Example function to load single list data
    fun loadSingleUsers() {
        viewModelScope.launch {
            _singleUserState.value = UserState.Loading
            when (val result = getUsersUseCase()) { // Call the single list use case
                is Success -> {
                    println("ViewModel: Received Success from GetUsersUseCase")
                    _singleUserState.value = UserState.Success(result.data)
                }
                is Failure -> {
                    println("ViewModel: Received Failure from GetUsersUseCase: $result")
                    // Handle different types of failures from the domain layer
                    _singleUserState.value = when (result) {
                        is Failure.ValidationError -> UserState.Error("Validation failed: ${result.errors.joinToString { it.message }}")
                        is Failure.NetworkError -> UserState.Error("Network error: ${result.message}")
                        is Failure.ServerError -> UserState.Error("Server error: ${result.message}")
                        is Failure.NoDataFound -> UserState.Error("Required data not found.") // Handle the specific business failure for empty list
                        is Failure.UnknownError -> UserState.Error("An unexpected error occurred: ${result.message}")
                    }
                }
            }
        }
    }

    // Example function to start observing the flow data
    fun startObservingUsers() {
        println("ViewModel: Starting to observe users flow")
        _flowUserState.value = UserState.Loading // Show loading initially

        observeUsersUseCase() // Get the Flow from the use case
            .onEach { result ->
                // Process each emitted Result from the flow
                println("ViewModel: Received emission from ObserveUsersUseCase Flow: $result")
                _flowUserState.value = when (result) {
                    is Success -> UserState.Success(result.data)
                    is Failure -> when (result) {
                        is Failure.ValidationError -> UserState.Error("Validation failed for update: ${result.errors.joinToString { it.message }}")
                        is Failure.NetworkError -> UserState.Error("Network error during updates: ${result.message}")
                        is Failure.ServerError -> UserState.Error("Server error during updates: ${result.message}")
                        is Failure.NoDataFound -> UserState.Error("Observed list is empty.") // Handle empty list emission failure
                        is Failure.UnknownError -> UserState.Error("An unexpected update error occurred: ${result.message}")
                    }
                }
            }
            // Use launchIn(viewModelScope) to start collecting the flow
            .launchIn(viewModelScope) // This collector runs as long as the ViewModel is active
    }

    // Example function to stop observing (if needed, though launchIn is often sufficient)
    // fun stopObservingUsers() { viewModelScope.coroutineContext.cancelChildren() } // Cancels all jobs in the scope

    override fun onCleared() {
        super.onCleared()
        // No need to explicitly cancel flows started with launchIn(viewModelScope)
        println("ViewModel cleared")
    }
}

// Sealed class to represent the state of the UI
sealed class UserState {
    object Idle : UserState() // Initial state
    object Loading : UserState()
    data class Success(val users: List<User>) : UserState()
    data class Error(val message: String) : UserState()
}
