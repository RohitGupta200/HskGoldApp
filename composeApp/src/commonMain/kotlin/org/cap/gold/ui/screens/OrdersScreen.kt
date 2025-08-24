package org.cap.gold.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.ui.screens.orders.OrderUiModel
import org.cap.gold.ui.screens.orders.OrdersUiState
import org.cap.gold.ui.screens.orders.OrdersViewModel
import org.cap.gold.util.formatAmount
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    uiState: OrdersUiState = OrdersUiState.Loading,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onOrderClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Wire ViewModel by default
    val vm = remember { OrdersViewModel() }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!started) {
            started = true
            vm.loadOrders()
        }
    }
    val state = vm.uiState

    OrdersScreenContent(
        uiState = if (state is OrdersUiState.Loading && uiState !is OrdersUiState.Loading) uiState else state,
        onBack = onBack,
        onRefresh = { vm.refresh() },
        onOrderClick = onOrderClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreenContent(
    onBack: () -> Unit,
    uiState: OrdersUiState,
    onRefresh: () -> Unit,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Active", "Past")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color.Gray
                    )
                }
            }
            
            // Order List
            when (uiState) {
                is OrdersUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is OrdersUiState.Success -> {
                    val orders = if (selectedTabIndex == 0) {
                        // Active: Pending, Confirmed, Shipped
                        uiState.orders.filter { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.SHIPPED }
                    } else {
                        // Past: Delivered, Cancelled
                        uiState.orders.filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED }
                    }
                    
                    if (orders.isEmpty()) {
                        EmptyOrdersView(selectedTabIndex == 0)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(orders) { order ->
                                OrderItem(
                                    order = order,
                                    onClick = { onOrderClick(order.id) }
                                )
                            }
                        }
                    }
                }
                is OrdersUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Failed to load orders. Tap to retry.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onRefresh() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    order: OrderUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Order ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (order.status) {
                                OrderStatus.PENDING -> Color(0xFFFFF3E0)
                                OrderStatus.CONFIRMED -> Color(0xFFE8F5E9)
                                OrderStatus.CANCELLED -> Color(0xFFFFEBEE)
                                OrderStatus.SHIPPED -> Color(0xFFE3F2FD)
                                OrderStatus.DELIVERED -> Color(0xFFEDE7F6)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (order.status) {
                                OrderStatus.PENDING -> Icons.Default.Pending
                                OrderStatus.CONFIRMED -> Icons.Default.CheckCircle
                                OrderStatus.CANCELLED -> Icons.Default.Cancel
                                OrderStatus.SHIPPED -> Icons.AutoMirrored.Filled.ArrowForward
                                OrderStatus.DELIVERED -> Icons.Default.DoneAll
                            },
                            contentDescription = null,
                            tint = when (order.status) {
                                OrderStatus.PENDING -> Color(0xFFFFA000)
                                OrderStatus.CONFIRMED -> Color(0xFF4CAF50)
                                OrderStatus.CANCELLED -> Color(0xFFF44336)
                                OrderStatus.SHIPPED -> Color(0xFF1E88E5)
                                OrderStatus.DELIVERED -> Color(0xFF5E35B1)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = order.status.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = when (order.status) {
                                OrderStatus.PENDING -> Color(0xFFFFA000)
                                OrderStatus.CONFIRMED -> Color(0xFF4CAF50)
                                OrderStatus.CANCELLED -> Color(0xFFF44336)
                                OrderStatus.SHIPPED -> Color(0xFF1E88E5)
                                OrderStatus.DELIVERED -> Color(0xFF5E35B1)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Product Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Product Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Qty: ${order.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "₹${formatAmount(order.totalAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Order Date and View Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Text(
                    text = "View Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EmptyOrdersView(isActiveTab: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isActiveTab) "No Active Orders" else "No Past Orders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isActiveTab) 
                "You don't have any active orders at the moment." 
            else 
                "Your past orders will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
