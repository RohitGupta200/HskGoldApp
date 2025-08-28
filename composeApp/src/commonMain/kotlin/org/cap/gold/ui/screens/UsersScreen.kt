package org.cap.gold.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cap.gold.ui.components.ErrorView
import org.cap.gold.ui.components.AppSearchBar
import org.cap.gold.ui.screens.admin.AdminUserListItem
import org.cap.gold.ui.screens.admin.UserListItem
import org.cap.gold.ui.screens.admin.UsersUiState
import org.cap.gold.ui.screens.admin.UsersViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    uiState: UsersUiState = UsersUiState.Loading,
    onBackClick: () -> Unit = {},

    onSearch: (String) -> Unit = {}
) {
    val vm: UsersViewModel = koinInject()
    val state =  vm.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onUpdateUserRole: (String, Int, () -> Unit) -> Unit = { uid, newRole, onSuccess -> vm.updateUserRole(uid, newRole, onSuccess) }

    // Show snackbar when there's an error
    LaunchedEffect(state) {
        if (state is UsersUiState.Error) {
            snackbarHostState.showSnackbar(
                message = state.message,
                withDismissAction = true
            )
        }
    }
    val onRefreshClick: () -> Unit = { vm.loadUsers(reset = true) }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            UsersTopAppBar(
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                isLoading = state is UsersUiState.Loading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            var searchQuery by remember { mutableStateOf("") }

            AppSearchBar(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                },
                onSearch = { vm.loadUsers(reset = true, search = searchQuery) },
                placeholder = "Search",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val state = state) {
                is UsersUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UsersUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = onRefreshClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
                is UsersUiState.Success -> {
                    if (state.users.isEmpty()) {
                        EmptyUsersView(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    } else {
                        val listState = rememberLazyListState()

                        // Determine when to load more: when last visible item is within 3 of the end
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val total = listState.layoutInfo.totalItemsCount
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                total > 0 && lastVisible >= total - 3
                            }
                        }

                        LaunchedEffect(shouldLoadMore, state.users.size) {
                            if (shouldLoadMore && vm.canLoadMore()) {
                                vm.loadMore()
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                            state = listState
                        ) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            items(state.users, key = { it.id }) { user ->
                                UserListItem(
                                    user = user,
                                    onRoleChange = { userId, newRole ->
                                        onUpdateUserRole(userId, newRole) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "User role updated successfully",
                                                    withDismissAction = true
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            // Loading footer
                            if (vm.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            } else {
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersTopAppBar(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            title = {
                Text(
                    text = "User Management",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            },
            actions = {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyUsersView(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No users found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your search or check back later",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
