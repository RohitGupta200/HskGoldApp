package org.cap.gold.ui.screens.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cap.gold.data.model.Product
import org.cap.gold.model.User
import org.cap.gold.ui.components.LoadingIndicator
import org.cap.gold.data.repository.CategoryRepository
import org.cap.gold.data.network.NetworkResponse
import org.koin.compose.koinInject
import org.cap.gold.platform.rememberImagePicker
import org.cap.gold.ui.components.LocalAddFieldDialogState
import org.cap.gold.ui.components.LocalStatusDialogState
import org.cap.gold.ui.components.StatusDialog
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Variant type for admin editing
enum class VariantType { APPROVED, UNAPPROVED }

@Composable
private fun EditableHeadline(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth()
        ) { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            inner()
        }
    }
}

@Composable
private fun EditableRowWithCheckBox(
    label: String,
    leftText: String,
    checked: Boolean,
    rightText: String,
    onCheckedChange: (Boolean) -> Unit,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = leftText,
                    onValueChange = onChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy( color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                ) { inner ->
                    if (leftText.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                }



                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    rightText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        DottedDivider()
    }
}

@Composable
private fun EditableMultiline(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth()
        ) { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            inner()
        }
    }
}

@Composable
private fun EditableDetailRow(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.widthIn(min = 120.dp)) {
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                ) { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                    inner()
                }
            }
        }
        DottedDivider()
    }
}

@Composable
private fun EditableDropdownRow(
    label: String,
    valueText: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { expanded = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelected(opt)
                    }
                )
            }
        }
        DottedDivider()
    }
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
                .menuAnchor()
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
private fun QuantitySelector(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    maxQuantity: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Quantity")
        Row(verticalAlignment = Alignment.CenterVertically
        , modifier = Modifier.padding(horizontal = 10.dp)) {
            IconButton(onClick = onDecrement, enabled = quantity > 1) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onIncrement, enabled = quantity < maxQuantity) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

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
    val statusDialog = LocalStatusDialogState.current
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
    
    // Show loading indicator (respect system bars)
    if (isLoading && product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
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
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (viewModel.isAdmin) (product?.name ?: "Product Details") else "Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.isAdmin && !viewModel.isCreateMode) {
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
                        viewModel.updateProduct(updated) {
                            CoroutineScope(Dispatchers.IO).launch {
                                statusDialog.show(
                                    success = true,
                                    message = "Product Updated Successfully",
                                )
                                delay(1000)
                                statusDialog.hide()
                            }
                            onProductUpdated() }
                    },
                    modifier = bodyModifier,
                    viewModel = viewModel,
                    user = user,
                    onPlaceOrder = { phone ->
                        val customerName = user.name ?: user.displayName ?: ""
                        viewModel.placeOrder(phone, customerName) { onOrderSuccess(it.id) }
                    },
                    onProductDeleted = {
                        CoroutineScope(Dispatchers.IO).launch {
                            statusDialog.show(
                                success = true,
                                message = "Product Deleted Successfully",
                            )
                            delay(1000)
                            statusDialog.hide()
                        }
                        onProductDeleted
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
                        viewModel.placeOrder(phone, customerName) {
                            CoroutineScope(Dispatchers.IO).launch {
                                statusDialog.show(
                                    success = true,
                                    message = "Order Placed Successfully",
                                    subMessage = "Our team will call you shortly"
                                )
                                delay(1000)
                                statusDialog.hide()
                            }
                            onOrderSuccess(it.id) }
                    },
                    onProductDeleted = { }
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
    onPlaceOrder: (String) -> Unit,
    onProductDeleted: () -> Unit
) {
    val addField = LocalAddFieldDialogState.current
    val statusDialog = LocalStatusDialogState.current
    // Admin can edit inline without toggling edit mode
    var activeType by remember { mutableStateOf(VariantType.APPROVED) }
    var approvedDraft by remember { mutableStateOf(product) }
    var unapprovedDraft by remember { mutableStateOf(product) }
    // Use viewModel.fields (SnapshotStateList) directly so mutations recompose
    val quantity = viewModel.quantity

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Product Image
        @OptIn(ExperimentalEncodingApi::class)
        val base64 = product.imageBase64
        val model: Any? = when {
            !base64.isNullOrEmpty() -> runCatching { Base64.decode(base64) }.getOrNull()
            product.imageUrl.isNotEmpty() -> product.imageUrl
            else -> null
        }
        val painter = rememberAsyncImagePainter(model = model)
        
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
            
            if (isAdmin) {
                val picker = rememberImagePicker(
                    onImagePicked = { bytes, fileName -> viewModel.onImageSelected(bytes, fileName) },
                    onError = { e -> viewModel.error = e.message ?: "Failed to pick image" }
                )
                Button(
                    onClick = { picker.pickImage() },
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
        if (isAdmin) {
            // Name and Description just below the image (borderless)
            val draft = if (activeType == VariantType.APPROVED) approvedDraft else unapprovedDraft
            Spacer(modifier = Modifier.height(12.dp))
            EditableHeadline(
                value = draft.name,
                placeholder = "Add item name",
                onChange = {
                    approvedDraft = approvedDraft.copy(name = it)
                    unapprovedDraft = unapprovedDraft.copy(name = it)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))
            EditableMultiline(
                value = draft.description,
                placeholder = "Enter item description",
                onChange = {
                    approvedDraft = approvedDraft.copy(description = it)
                    unapprovedDraft = unapprovedDraft.copy(description = it)
                }
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            DottedDivider()
            Spacer(Modifier.height(8.dp))

            // Type dropdown to switch editing variant (flat style)
            EditableDropdownRow(
                label = "Type",
                valueText = if (activeType == VariantType.APPROVED) "Approved" else "Unapproved",
                options = listOf("Approved", "Unapproved"),
                onSelected = { sel ->
                    activeType =
                        if (sel == "Approved") VariantType.APPROVED else VariantType.UNAPPROVED
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Editable numeric/text detail fields as label : value


            // Sync price option: when enabled, unapproved.price will be set to approved.price on save
            EditableRowWithCheckBox(
                label = "Price",
                leftText = draft.price.takeIf { !it.isNaN() }?.toString() ?: "",
                checked = viewModel.syncPrices,
                rightText = "Apply To All",
                onCheckedChange = { viewModel.syncPrices = it },
                onChange = { v ->
                    val d = v.toDoubleOrNull() ?: 0.0
                    if (activeType == VariantType.APPROVED) approvedDraft =
                        draft.copy(price = d) else unapprovedDraft = draft.copy(price = d)
                },
                placeholder = "Add price",
                keyboardType = KeyboardType.Number,
            )

            Spacer(modifier = Modifier.height(8.dp))

            EditableDetailRow(
                label = "Purity",
                value = draft.purity,
                placeholder = "Add purity",
                onChange = {
                    if (activeType == VariantType.APPROVED) approvedDraft =
                        draft.copy(purity = it) else unapprovedDraft = draft.copy(purity = it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            EditableDetailRow(
                label = "Weight",
                value = draft.weight,
                placeholder = "Add weight",
                keyboardType = KeyboardType.Text,
                onChange = { v ->
                    if (activeType == VariantType.APPROVED) approvedDraft =
                        draft.copy(weight = v) else unapprovedDraft = draft.copy(weight = v)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            EditableDetailRow(
                label = "Dimensions",
                value = draft.dimension,
                placeholder = "Add dimensions",
                onChange = {
                    if (activeType == VariantType.APPROVED) approvedDraft =
                        draft.copy(dimension = it) else unapprovedDraft = draft.copy(dimension = it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            EditableDetailRow(
                label = "Max quantity",
                value = draft.maxQuantity.toString(),
                placeholder = "Add max quantity",
                keyboardType = KeyboardType.Number,
                onChange = { v ->
                    val mq = v.toIntOrNull() ?: 1
                    if (activeType == VariantType.APPROVED) approvedDraft =
                        draft.copy(maxQuantity = mq) else unapprovedDraft =
                        draft.copy(maxQuantity = mq)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category at last (flat dropdown)
            EditableDropdownRow(
                label = "Category",
                valueText = draft.category.ifBlank { "Select" },
                options = categories,
                onSelected = { sel ->
                    if (activeType == VariantType.APPROVED) approvedDraft =
                        draft.copy(category = sel) else unapprovedDraft = draft.copy(category = sel)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            for(i in viewModel.fields.indices){
                ProductCustomRow(
                    viewModel.fields[i].label,
                    viewModel.fields[i].value,
                    isAdmin,
                    onRemoveField = { viewModel.removeField(i) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DottedDivider()
            }

            if(!(viewModel.fields.size>=5))
                Button(
                    onClick = { addField.show { label, value ->
                        viewModel.addField(label, value)
                    } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Add a Field", style = MaterialTheme.typography.titleMedium)
                }

            // Bottom actions styled like design: Delete (light) on top, Save primary below
            Spacer(modifier = Modifier.height(24.dp))
            if (!viewModel.isCreateMode) {
                Button(
                    onClick = { viewModel.deleteProduct(onProductDeleted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Delete item", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Enable Save only when required fields are set (admin): name, category, price
            val currentDraft = if (activeType == VariantType.APPROVED) approvedDraft else unapprovedDraft
            val canSave = currentDraft.name.isNotBlank() && currentDraft.category.isNotBlank() && !currentDraft.price.isNaN()
            Button(
                onClick = {
                    val approvedToSend = approvedDraft
                    val unapprovedToSend = unapprovedDraft
                    viewModel.upsertBothFromUi(
                        approved = approvedToSend,
                        unapproved = unapprovedToSend,
                        onSuccess = {
                            statusDialog.show(
                                success = true,
                                message = "Product saved successfully",
                                subMessage = null
                            )
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (viewModel.isCreateMode) "Add Item" else "Save",
                    style = MaterialTheme.typography.titleMedium
                )
            }

        } else {

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (product.description.isNotBlank()) {
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Price


            Spacer(modifier = Modifier.height(20.dp))

            // Details
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(8.dp))
            ProductDetailRow("Price", "₹${product.price}")
            DottedDivider()
            ProductDetailRow("Purity", product.purity)
            DottedDivider()
            ProductDetailRow("Weight", "${product.weight} ")
            DottedDivider()
            ProductDetailRow("Dimensions", product.dimension)
            DottedDivider()
            ProductDetailRow("Category", product.category)

            Spacer(modifier = Modifier.height(8.dp))
            for(i in viewModel.fields.indices){
                DottedDivider()
                ProductCustomRow( viewModel.fields[i].label, viewModel.fields[i].value, isAdmin, onRemoveField = { viewModel.removeField(i) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }




            // Admin actions removed; admin edits inline above
            
            // --- Order Section for non-admins ---
            if (!isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                DottedDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Quantity Selector
                QuantitySelector(
                    quantity = quantity,
                    onIncrement = { viewModel.incrementQuantity() },
                    onDecrement = { viewModel.decrementQuantity() },
                    maxQuantity = product.maxQuantity
                )
                if (quantity >= product.maxQuantity) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Maximum Quantity Reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                DottedDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Place Order Button
                Button(
                    onClick = { onPlaceOrder(user.phoneNumber) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    enabled = !viewModel.isLoading && product.maxQuantity > 0
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeDropdown(
    selected: VariantType,
    onSelected: (VariantType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (selected == VariantType.APPROVED) "Approved" else "Unapproved",
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Approved") },
                onClick = {
                    onSelected(VariantType.APPROVED)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Unapproved") },
                onClick = {
                    onSelected(VariantType.UNAPPROVED)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SegmentedButtons(
    active: VariantType,
    onChange: (VariantType) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable
        fun seg(text: String, selected: Boolean, onClick: () -> Unit) {
            val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = bg,
                    contentColor = fg
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text)
            }
        }

        seg("Approved", active == VariantType.APPROVED) { onChange(VariantType.APPROVED) }
        seg("Unapproved", active == VariantType.UNAPPROVED) { onChange(VariantType.UNAPPROVED) }
    }
}

@Composable
private fun ProductDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label + ":",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant

            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ProductCustomRow(label: String, value: String,isAdmin: Boolean,onRemoveField: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label + ":",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant

            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = if(!isAdmin) TextAlign.End else TextAlign.Center
            )
            if (isAdmin) {
                IconButton(onClick = { onRemoveField() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}



@Composable
private fun DottedDivider() {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val y = size.height / 2f
        drawLine(
            color = dividerColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
            strokeWidth = 2f
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
