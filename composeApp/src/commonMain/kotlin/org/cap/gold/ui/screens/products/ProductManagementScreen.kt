package org.cap.gold.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cap.gold.data.model.Product
import org.cap.gold.ui.components.ProductItem

/**
 * Screen for managing products (approval, editing, etc.)
 * Primarily used by admins to manage product listings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    userRole: Int,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = userRole == 0
    val isApprovedUser = userRole == 1
    
    // TODO: Replace with actual data from ViewModel
    val products = remember { 
        List(10) { index ->
            Product(
                id = index.toString(),
                name = "Product $index",
                price = 5000.0 + (index * 1000),
                weight = 5.0 + index,
                dimension = "${10 + index}mm",
                purity = "22",
                maxQuantity = 5,
                category = if (index % 2 == 0) "Ring" else "Chain",
                description = "Beautiful ${if (index % 2 == 0) "ring" else "chain"} made of 22K gold"
            )
        }
    }
    
    var showAddProductDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) "Manage Products" else "Our Collection") },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showAddProductDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Product")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = modifier.padding(padding)) {
            if (products.isEmpty()) {
                EmptyProductsView(isAdmin = isAdmin)
            } else {
                ProductGrid(
                    products = products,
                    onProductClick = onProductClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Add Product Dialog (Admin only)
        if (showAddProductDialog) {
            // TODO: Implement add product dialog
            AlertDialog(
                onDismissRequest = { showAddProductDialog = false },
                title = { Text("Add New Product") },
                text = { Text("Add product form will be implemented here") },
                confirmButton = {
                    TextButton(
                        onClick = { showAddProductDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(products, key = { it.id }) { product ->
            ProductItem(
                product = product,
                onProductClick = { onProductClick(product.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyProductsView(isAdmin: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isAdmin) "No products found" else "No products available",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isAdmin) "Tap + to add a new product" 
                  else "Please check back later for updates",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
