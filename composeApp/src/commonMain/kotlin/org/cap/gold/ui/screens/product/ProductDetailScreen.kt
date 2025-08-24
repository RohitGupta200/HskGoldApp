package org.cap.gold.ui.screens.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import org.cap.gold.data.model.Product
import org.cap.gold.model.User
import org.cap.gold.ui.components.LoadingIndicator
import org.cap.gold.data.repository.CategoryRepository
import org.cap.gold.data.network.NetworkResponse
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    user: User,
    onBackClick: () -> Unit,
    onOrderSuccess: (orderId: String) -> Unit,
    onProductUpdated: () -> Unit,
    onProductDeleted: () -> Unit
) {
    // Fetch categories for admin editing
    val categoryRepo = koinInject<CategoryRepository>()
    var categoryNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        when (val res = categoryRepo.getAll()) {
            is NetworkResponse.Success -> categoryNames = res.data.map { it.name }
            else -> {}
        }
    }

    val product = viewModel.product
    val isLoading = viewModel.isLoading
    val error = viewModel.error
    val orderSuccess = viewModel.orderSuccess
    
    // Show loading indicator
    if (isLoading && product == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
        return
    }
    
    // Show error message if any
    error?.let { errorMessage ->
        ErrorDialog(
            message = errorMessage,
            onDismiss = { viewModel.clearError() }
        )
    }
    
    // Show order success dialog
    if (orderSuccess) {
        SuccessDialog(
            title = "Order Placed Successfully!",
            message = "Your order has been placed successfully. We'll contact you soon.",
            onDismiss = { onOrderSuccess("") }
        )
    }
    
    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.isAdmin) {
                        IconButton(onClick = { viewModel.deleteProduct(onProductDeleted) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val bodyModifier = Modifier.padding(padding)
        product?.let { p ->
            if (viewModel.isAdmin) {
                ProductContent(
                    product = p,
                    isAdmin = true,
                    isApprovedUser = viewModel.isApprovedUser,
                    categories = categoryNames,
                    onProductUpdate = { updated ->
                        viewModel.updateProduct(updated) { onProductUpdated() }
                    },
                    modifier = bodyModifier,
                    viewModel = viewModel,
                    user = user,
                    onPlaceOrder = { phone ->
                        val customerName = user.name ?: user.displayName ?: ""
                        viewModel.placeOrder(phone, customerName) { onOrderSuccess(it.id) }
                    }
                )
            } else {
                ProductContent(
                    product = p,
                    isAdmin = false,
                    isApprovedUser = viewModel.isApprovedUser,
                    categories = emptyList(),
                    onProductUpdate = { /* no-op for non-admin */ },
                    modifier = bodyModifier,
                    viewModel = viewModel,
                    user = user,
                    onPlaceOrder = { phone ->
                        val customerName = user.name ?: user.displayName ?: ""
                        viewModel.placeOrder(phone, customerName) { onOrderSuccess(it.id) }
                    }
                )
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Product not found")
            }
        }
    }
}

@Composable
private fun ProductContent(
    product: Product,
    isAdmin: Boolean,
    isApprovedUser: Boolean,
    categories: List<String>,
    onProductUpdate: (Product) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel,
    user: User,
    onPlaceOrder: (String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var editedProduct by remember { mutableStateOf(product) }
    val quantity = viewModel.quantity

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Product Image
        val painter = rememberAsyncImagePainter(
            model = product.imageUrl.ifEmpty { null },
            error = rememberAsyncImagePainter(
                model = null,
                contentScale = ContentScale.Crop,
                error = null
            )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            if (isAdmin && isEditMode) {
                // TODO: Add image upload functionality
                Button(
                    onClick = { /* TODO: Implement image upload */ },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Edit, "Change Image")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Product Details
        if (isAdmin && isEditMode) {
            // Editable fields for admin mirroring read-only layout
            OutlinedTextField(
                value = editedProduct.name,
                onValueChange = { editedProduct = editedProduct.copy(name = it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.imageUrl,
                onValueChange = { editedProduct = editedProduct.copy(imageUrl = it) },
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.price.toString(),
                onValueChange = { v ->
                    val d = v.toDoubleOrNull() ?: 0.0
                    editedProduct = editedProduct.copy(price = d)
                },
                label = { Text("Price (₹)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.weight.toString(),
                onValueChange = { v ->
                    editedProduct = editedProduct.copy(weight = v.toDoubleOrNull() ?: 0.0)
                },
                label = { Text("Weight (g)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.purity,
                onValueChange = { editedProduct = editedProduct.copy(purity = it) },
                label = { Text("Purity") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.dimension,
                onValueChange = { editedProduct = editedProduct.copy(dimension = it) },
                label = { Text("Dimension") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.maxQuantity.toString(),
                onValueChange = { v ->
                    editedProduct = editedProduct.copy(maxQuantity = v.toIntOrNull() ?: 1)
                },
                label = { Text("Max Quantity") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category dropdown (fetched from API)
            CategoryDropdown(
                categories = categories,
                selected = editedProduct.category,
                onSelected = { editedProduct = editedProduct.copy(category = it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editedProduct.description,
                onValueChange = { editedProduct = editedProduct.copy(description = it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Save/Cancel buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { isEditMode = false; editedProduct = product }) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onProductUpdate(editedProduct)
                        isEditMode = false
                    }
                ) {
                    Text("Save Changes")
                }
            }
        } else {
            // Read-only view
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price
            Text(
                text = "₹${product.price}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details
            ProductDetailRow("Weight", "${product.weight}g")
            ProductDetailRow("Purity", "${product.purity}K")
            ProductDetailRow("Dimension", product.dimension)
            ProductDetailRow("Category", product.category)
            ProductDetailRow("Max Quantity", product.maxQuantity.toString())
            
            // Description
            if (product.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = product.description)
            }
            
            // Admin actions
            if (isAdmin) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { isEditMode = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, "Edit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Product")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Approve button (only for unapproved products)
            }
            
            // --- Order Section for non-admins ---
            if (!isAdmin) {
                Spacer(modifier = Modifier.height(24.dp))

                // Quantity Selector
                QuantitySelector(
                    quantity = quantity,
                    onIncrement = { viewModel.incrementQuantity() },
                    onDecrement = { viewModel.decrementQuantity() },
                    maxQuantity = product.maxQuantity
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Place Order Button
                Button(
                    onClick = { onPlaceOrder(user.phoneNumber) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.ShoppingCart, "Place Order")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Place Order")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    maxQuantity: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Quantity:", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, enabled = quantity > 1) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease quantity")
            }
            Text(quantity.toString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp))
            IconButton(onClick = onIncrement, enabled = quantity < maxQuantity) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase quantity")
            }
        }
    }
    if (quantity >= maxQuantity) {
        Text(
            "Maximum quantity reached",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDualEditContent(
    initial: Product?,
    onSave: (approved: Product?, unapproved: Product?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Draft states for both variants
    var selectedTab by remember { mutableStateOf(0) } // 0 = Approved, 1 = Unapproved
    var approvedDraft by remember {
        mutableStateOf(
            initial ?: Product(
                id = "",
                name = "",
                price = 0.0,
                imageUrl = "",
                category = "",
                description = "",
                weight = 0.0,
                purity = "",
                dimension = "",
                maxQuantity = 1
            )
        )
    }
    var unapprovedDraft by remember { mutableStateOf(approvedDraft.copy()) }
    var showSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Approved") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Unapproved") })
        }

        Spacer(Modifier.height(12.dp))

        // Editor for selected variant
        val onDraftChange: (Product) -> Unit = { p ->
            if (selectedTab == 0) approvedDraft = p else unapprovedDraft = p
        }
        VariantEditor(
            product = if (selectedTab == 0) approvedDraft else unapprovedDraft,
            onChange = onDraftChange
        )

        Spacer(Modifier.height(16.dp))

        // Save actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                onSave(approvedDraft, unapprovedDraft)
                showSuccess = true
            }) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Both Variants")
            }
        }
    }

    if (showSuccess) {
        SuccessDialog(
            title = "Saved",
            message = "Product variants saved successfully.",
            onDismiss = { showSuccess = false }
        )
    }
}

@Composable
private fun VariantEditor(
    product: Product,
    onChange: (Product) -> Unit
) {
    // Category options (placeholder list; replace with dynamic categories when available)
    val categories = listOf("Ring", "Necklace", "Bracelet", "Earring", "Pendant")

    OutlinedTextField(
        value = product.name,
        onValueChange = { onChange(product.copy(name = it)) },
        label = { Text("Name") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = product.price.toString(),
        onValueChange = { v ->
            val d = v.toDoubleOrNull() ?: 0.0
            onChange(product.copy(price = d))
        },
        label = { Text("Price (₹)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = product.weight.toString(),
        onValueChange = { v -> onChange(product.copy(weight = v.toDoubleOrNull() ?: 0.0)) },
        label = { Text("Weight (g)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = product.purity,
        onValueChange = { onChange(product.copy(purity = it)) },
        label = { Text("Purity") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = product.dimension,
        onValueChange = { onChange(product.copy(dimension = it)) },
        label = { Text("Dimension") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = product.maxQuantity.toString(),
        onValueChange = { v -> onChange(product.copy(maxQuantity = v.toIntOrNull() ?: 1)) },
        label = { Text("Max Quantity") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    CategoryDropdown(
        categories = categories,
        selected = product.category,
        onSelected = { onChange(product.copy(category = it)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Error")
            }
        },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
