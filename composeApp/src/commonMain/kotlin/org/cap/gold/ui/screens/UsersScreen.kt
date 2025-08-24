package org.cap.gold.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cap.gold.ui.components.ErrorView
import org.cap.gold.ui.screens.admin.UserListItem
import org.cap.gold.ui.screens.admin.UsersUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    uiState: UsersUiState = UsersUiState.Loading,
    onBackClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onUpdateUserRole: (String, Int, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar when there's an error
    LaunchedEffect(uiState) {
        if (uiState is UsersUiState.Error) {
            snackbarHostState.showSnackbar(
                message = uiState.message,
                withDismissAction = true
            )
        }
    }

    Scaffold(
        topBar = {
            UsersTopAppBar(
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                isLoading = uiState is UsersUiState.Loading
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

            DockedSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { query ->
                            searchQuery = query
                            onSearch(query)
                        },
                        onSearch = { onSearch(searchQuery) },
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("Search users...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search users",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
            ) {
                // Search suggestions would go here
            }

            when (val state = uiState) {
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
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            items(state.users) { user ->
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
                            item { Spacer(modifier = Modifier.height(8.dp)) }
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
    TopAppBar(
        title = {
            Text(
                text = "User Management",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
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
