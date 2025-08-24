package org.cap.gold.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cap.gold.data.model.Category
import org.cap.gold.data.network.NetworkResponse
import org.cap.gold.data.repository.CategoryRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = koinInject<CategoryRepository>()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        refresh(repo, onLoaded = { categories = it }, onError = { error = it }, onDone = { isLoading = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New category name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(enabled = newName.isNotBlank(), onClick = {
                    scope.launch {
                        when (val res = repo.create(newName.trim())) {
                            is NetworkResponse.Success -> {
                                newName = ""
                                refresh(repo, onLoaded = { categories = it }, onError = { error = it })
                            }
                            is NetworkResponse.Error -> error = res.message
                            else -> {}
                        }
                    }
                }) { Text("Add") }
            }

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(text = error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    LazyColumn {
                        items(categories, key = { it.id }) { cat ->
                            CategoryRow(cat, onDelete = { id ->
                                scope.launch {
                                    when (val res = repo.delete(id)) {
                                        is NetworkResponse.Success -> refresh(repo, onLoaded = { categories = it }, onError = { error = it })
                                        is NetworkResponse.Error -> error = res.message
                                        else -> {}
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onDelete: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.name, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onDelete(category.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

private suspend fun refresh(
    repo: CategoryRepository,
    onLoaded: (List<Category>) -> Unit,
    onError: (String?) -> Unit,
    onDone: (() -> Unit)? = null
) {
    when (val res = repo.getAll()) {
        is NetworkResponse.Success -> onLoaded(res.data)
        is NetworkResponse.Error -> onError(res.message)
        else -> {}
    }
    onDone?.invoke()
}
