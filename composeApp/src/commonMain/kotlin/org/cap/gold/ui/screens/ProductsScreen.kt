package org.cap.gold.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cap.gold.ui.components.AppSearchBar
import org.cap.gold.ui.navigation.ManageCategoriesVoyagerScreen
import org.cap.gold.ui.navigation.ProductDetailVoyagerScreen
import org.cap.gold.ui.navigation.AppRoute
import org.cap.gold.ui.navigation.LocalAppNavigator
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch
import org.cap.gold.data.model.Product
import org.cap.gold.data.network.NetworkResponse
import org.cap.gold.data.repository.ProductRepository
import org.cap.gold.model.User
import org.cap.gold.ui.components.BrandingImage
import org.cap.gold.ui.components.ProductItem
import org.cap.gold.ui.screens.product.ProductDetailScreen
import org.cap.gold.ui.screens.product.ProductDetailViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    user: User,
    navigator: Navigator? = LocalNavigator.current
) {
    val appNavigator = LocalAppNavigator.current
    val userRole = user.role
    val onProductClick: (String) -> Unit = { productId ->
        if (appNavigator != null) {
            appNavigator.push(AppRoute.ProductDetail(productId, user))
        } else {
            navigator?.push(ProductDetailVoyagerScreen(productId = productId, user = user))
        }
    }

    val productRepository = koinInject<ProductRepository> { parametersOf(userRole) }
    var searchQuery by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load products when the screen is first shown or when userRole changes
    LaunchedEffect(userRole) {
        isLoading = true
        errorMessage = null
        try {
            val res = if (userRole == 2) {
                productRepository.getUnapprovedProducts()
            } else {
                productRepository.getApprovedProducts()
            }
            when (res) {
                is NetworkResponse.Loading -> {
                    isLoading = true
                }
                is NetworkResponse.Success -> {
                    products = res.data
                    isLoading = false
                    errorMessage = null
                }
                is NetworkResponse.Error -> {
                    errorMessage = res.message
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load products: ${e.message}"
            isLoading = false
        }
    }

    // Filter and group products by category
    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) {
            products.sortedBy { it.name }.groupBy { it.category }
        } else {
            val query = searchQuery.lowercase()
            products.filter { product ->
                product.name.lowercase().contains(query) ||
                (product.description?.lowercase()?.contains(query) ?: false) ||
                product.category.lowercase().contains(query)
            }.groupBy { it.category }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandingImage(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 15.dp),

            )
            // Search bar
            AppSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Show loading indicator while loading
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Show error message if there was an error
            if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error loading products",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    errorMessage = null
                                    isLoading = true
                                    try {
                                        val res = if (userRole == 2) {
                                            productRepository.getUnapprovedProducts()
                                        } else {
                                            productRepository.getApprovedProducts()
                                        }
                                        when (res) {
                                            is NetworkResponse.Loading -> {
                                                isLoading = true
                                            }

                                            is NetworkResponse.Success -> {
                                                products = res.data
                                                isLoading = false
                                            }

                                            is NetworkResponse.Error -> {
                                                errorMessage = res.message
                                                isLoading = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to load products: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
                return@Column
            }

            // Show empty state if no products
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (userRole == 2) "No products found" else "No products available",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (userRole == 0) {
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = { onProductClick("new") }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Product")
                            }
                        }
                    }
                }
                return@Column
            }

            // Display products in a grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredProducts.forEach { (category, categoryProducts) ->
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (userRole == 0) {
                                TextButton(onClick = { onProductClick("new") }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }
                        }
                    }

                    items(categoryProducts) { product ->
                        ProductItem(
                            product = product,
                            onProductClick = onProductClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        if (userRole == 0) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (appNavigator != null) {
                        appNavigator.push(AppRoute.ManageCategories)
                    } else {
                        navigator?.push(ManageCategoriesVoyagerScreen())
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Manage Categories") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

 

@Preview
@Composable
fun ProductsScreenPreview() {
    MaterialTheme {
        Surface {
            ProductsScreen(
                user = User(
                    id = "preview_user",
                    email = "preview@example.com",
                    phoneNumber = "1234567890",
                    role = 0
                )
            )
        }
    }
}