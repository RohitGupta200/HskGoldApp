package org.cap.gold.ui.screens.admin.local

import org.cap.gold.model.User

sealed class LocalUsersUiState {
    data object Loading : LocalUsersUiState()
    data class Error(val message: String) : LocalUsersUiState()
    data class Success(val users: List<User>) : LocalUsersUiState()
}
