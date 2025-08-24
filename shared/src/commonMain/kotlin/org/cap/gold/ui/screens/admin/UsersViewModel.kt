package org.cap.gold.ui.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cap.gold.data.repository.UserRepository
import org.cap.gold.model.User
import org.cap.gold.util.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val users: List<User>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

class UsersViewModel : KoinComponent {
    private val userRepository: UserRepository by inject()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    var uiState: UsersUiState by mutableStateOf(UsersUiState.Loading)
        private set
    
    private var allUsers: List<User> = emptyList()
    
    init {
        loadUsers()
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            uiState = UsersUiState.Loading
            try {
                when (val result = userRepository.getUsers()) {
                    is Result.Success -> {
                        allUsers = result.data
                        uiState = UsersUiState.Success(allUsers)
                    }
                    is Result.Error -> {
                        uiState = UsersUiState.Error(result.message)
                    }
                    is Result.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                uiState = UsersUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    fun updateUserRole(userId: String, newRole: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                when (val result = userRepository.updateUserRole(userId, newRole)) {
                    is Result.Success -> {
                        // Update the local list
                        allUsers = allUsers.map { user ->
                            if (user.id == userId) user.copy(role = newRole)
                            else user
                        }
                        uiState = UsersUiState.Success(allUsers)
                        onSuccess()
                    }
                    is Result.Error -> {
                        // Could show an error message
                        uiState = UsersUiState.Error(result.message)
                    }
                    is Result.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                uiState = UsersUiState.Error(e.message ?: "Failed to update user role")
            }
        }
    }
    
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            uiState = UsersUiState.Success(allUsers)
        } else {
            val filtered = allUsers.filter { user ->
                user.phoneNumber.contains(query, ignoreCase = true) ||
                user.displayName?.contains(query, ignoreCase = true) == true ||
                user.email?.contains(query, ignoreCase = true) == true
            }
            uiState = UsersUiState.Success(filtered)
        }
    }
}

