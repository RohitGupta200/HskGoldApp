package org.cap.gold.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.ui.components.LoadingIndicator
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    orderId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    // Show error message if any (temporary println; replace with Snackbar as needed)
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            println("Error: $error")
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Order Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.order != null -> {
                OrderDetailContent(
                    order = uiState.order!!,
                    onStatusChange = { newStatus ->
                        viewModel.updateOrderStatus(newStatus)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        // Confirmation dialog for status change
        if (uiState.showStatusUpdateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissStatusUpdateDialog() },
                title = { Text("Confirm Status Change") },
                text = { 
                    Text("Are you sure you want to change the order status to ${uiState.newStatus?.name}?")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmStatusUpdate() }) {
                        Text("CONFIRM")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissStatusUpdateDialog() }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        // Simple transient message handling
        if (uiState.message != null) {
            LaunchedEffect(uiState.message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearMessage()
            }
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text(uiState.message!!)
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: Order,
    onStatusChange: (OrderStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order ID Section
        Text(
            text = "Order ID: #${order.id.takeLast(6)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        
        // Order Details
        OrderDetailRow("Order date", order.createdAt)
        OrderDetailRow(
            "Order status", 
            order.status.name, 
            valueColor = when (order.status) {
                OrderStatus.PENDING -> Color(0xFFFFA000)
                OrderStatus.CONFIRMED -> Color(0xFF4CAF50)
                OrderStatus.CANCELLED -> Color(0xFFF44336)
                OrderStatus.SHIPPED -> Color(0xFF1E88E5)
                OrderStatus.DELIVERED -> Color(0xFF5E35B1)
            },
            showStatusButtons = order.status == OrderStatus.PENDING,
            onStatusChange = onStatusChange
        )
        OrderDetailRow("Order from", order.name.ifBlank { "Customer" })
        OrderDetailRow("Total amount", "₹${order.totalPrice}")
        OrderDetailRow("Order contact", order.phoneNumber)
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        
        // Items Section
        Text(
            text = "Item #1",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Item details (Mock data - in real app this would be from product info)
        OrderDetailRow("", "24K Gold × 1")
        OrderDetailRow("Weight", "100 grams")
        OrderDetailRow("Dimensions", "50 mm x 30 mm x 5 mm")
        OrderDetailRow("Amount", "₹${order.totalPrice}")
    }
}

@Composable
private fun OrderDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showStatusButtons: Boolean = false,
    onStatusChange: ((OrderStatus) -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (showStatusButtons && label == "Order status") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStatusChange?.invoke(OrderStatus.CANCELLED) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Reject", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { onStatusChange?.invoke(OrderStatus.CONFIRMED) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Accept", fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                    textAlign = TextAlign.End
                )
            }
        }
        
        if (label.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                thickness = 0.5.dp
            )
        }
    }
}
