package org.cap.gold.ui.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cap.gold.data.remote.UsersApiService
import org.cap.gold.model.User

sealed class UsersUiState {
    data object Loading : UsersUiState()
    data class Error(val message: String) : UsersUiState()
    data class Success(
        val users: List<User>,
        val nextPageToken: String?
    ) : UsersUiState()
}

class UsersViewModel(
    private val usersApi: UsersApiService
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    var uiState: UsersUiState by mutableStateOf<UsersUiState>(UsersUiState.Loading)
        private set

    private var nextPageToken: String? = null
    private val users = mutableListOf<User>()
    var isLoadingMore by mutableStateOf(false)
        private set

    init {
        loadUsers(reset = true)
    }

    fun loadUsers(reset: Boolean = false, search: String? = null) {
        if (reset) {
            uiState = UsersUiState.Loading
            nextPageToken = null
            users.clear()
            isLoadingMore = false
        }
        scope.launch {
            try {
                val page = usersApi.listUsers(pageToken = if (reset) null else nextPageToken, search = search)
                if (reset) users.clear()
                users.addAll(page.users)
                nextPageToken = page.nextPageToken
                uiState = UsersUiState.Success(users = users.toList(), nextPageToken = nextPageToken)
            } catch (e: Exception) {
                uiState = UsersUiState.Error(e.message ?: "Failed to load users")
            }
        }
    }

    fun loadMore() {
        val token = nextPageToken ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        // Keep current list while loading next page
        scope.launch {
            try {
                val page = usersApi.listUsers(pageToken = token)
                users.addAll(page.users)
                nextPageToken = page.nextPageToken
                uiState = UsersUiState.Success(users = users.toList(), nextPageToken = nextPageToken)
            } catch (e: Exception) {
                uiState = UsersUiState.Error(e.message ?: "Failed to load more users")
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun updateUserRole(userId: String, newRole: Int, onSuccess: () -> Unit = {}) {
        scope.launch {
            try {
                val updated = usersApi.updateUserRole(userId, newRole)
                val idx = users.indexOfFirst { it.id == userId }
                if (idx >= 0) {
                    users[idx] = updated
                    uiState = UsersUiState.Success(users = users.toList(), nextPageToken = nextPageToken)
                }
                onSuccess()
            } catch (e: Exception) {
                uiState = UsersUiState.Error(e.message ?: "Failed to update role")
            }
        }
    }

    fun canLoadMore(): Boolean = nextPageToken != null && !isLoadingMore
}
