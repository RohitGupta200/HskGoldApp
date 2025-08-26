package org.cap.gold.ui.screens.admin.legacy

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

sealed interface LegacyUsersUiState {
    data object Loading : LegacyUsersUiState
    data class Success(val users: List<User>) : LegacyUsersUiState
    data class Error(val message: String) : LegacyUsersUiState
}

class LegacyUsersViewModel : KoinComponent {
    private val userRepository: UserRepository by inject()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    var uiState: LegacyUsersUiState by mutableStateOf(LegacyUsersUiState.Loading)
        private set
    
    private var allUsers: List<User> = emptyList()
    
    init {
        loadUsers()
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            uiState = LegacyUsersUiState.Loading
            try {
                when (val result = userRepository.getUsers()) {
                    is Result.Success -> {
                        allUsers = result.data
                        uiState = LegacyUsersUiState.Success(allUsers)
                    }
                    is Result.Error -> {
                        uiState = LegacyUsersUiState.Error(result.message)
                    }
                    is Result.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                uiState = LegacyUsersUiState.Error(e.message ?: "An unexpected error occurred")
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
                        uiState = LegacyUsersUiState.Success(allUsers)
                        onSuccess()
                    }
                    is Result.Error -> {
                        uiState = LegacyUsersUiState.Error(result.message)
                    }
                    is Result.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                uiState = LegacyUsersUiState.Error(e.message ?: "Failed to update user role")
            }
        }
    }
    
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            uiState = LegacyUsersUiState.Success(allUsers)
        } else {
            val filtered = allUsers.filter { user ->
                user.phoneNumber.contains(query, ignoreCase = true) ||
                user.displayName?.contains(query, ignoreCase = true) == true ||
                user.email?.contains(query, ignoreCase = true) == true
            }
            uiState = LegacyUsersUiState.Success(filtered)
        }
    }
}

