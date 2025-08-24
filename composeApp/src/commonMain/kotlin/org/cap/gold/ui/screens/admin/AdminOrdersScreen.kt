package org.cap.gold.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.ui.screens.orders.OrderUiModel
import org.cap.gold.ui.screens.orders.OrdersUiState
import org.cap.gold.ui.screens.order.OrderDetailScreen
import org.cap.gold.ui.screens.order.OrderDetailViewModel
import org.cap.gold.util.formatAmount
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
 
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    navigator: Navigator? = LocalNavigator.current,
    onBack: () -> Unit = { navigator?.pop() },
    modifier: Modifier = Modifier
) {
    // Default wrapper now wires the ViewModel automatically
    val vm: AdminOrdersViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val isLoadingMore by vm.isLoadingMore.collectAsState()

    AdminOrdersScreenContent(
        uiState = state,
        onBack = onBack,
        onRefresh = { vm.loadOrders(reset = true) },
        onStatusChange = { id, st -> vm.updateOrderStatus(id, st) },
        onOrderClick = { orderId ->
            navigator?.push(object : Screen {
                @Composable
                override fun Content() {
                    val viewModel: OrderDetailViewModel = koinInject(
                        parameters = { parametersOf(orderId) }
                    )
                    OrderDetailScreen(
                        viewModel = viewModel,
                        orderId = orderId,
                        onBack = { navigator.pop() }
                    )
                }
            })
        },
        onSearch = { q -> vm.search(q) },
        onLoadMore = { vm.loadMore() },
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreenContent(
    onBack: () -> Unit,
    uiState: OrdersUiState,
    onRefresh: () -> Unit,
    onStatusChange: (String, OrderStatus) -> Unit,
    onOrderClick: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showStatusDialog by remember { mutableStateOf<OrderUiModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    // Debounce search
    LaunchedEffect(searchQuery) {
        delay(350)
        onSearch(searchQuery)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search by order, user, product…") },
                singleLine = true
            )

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
                if (uiState.orders.isEmpty()) {
                    EmptyAdminOrdersView()
                } else {
                    LazyColumn(
                        modifier = modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.orders) { order ->
                            AdminOrderItem(
                                order = order,
                                onStatusClick = { showStatusDialog = order },
                                onClick = { onOrderClick(order.id) }
                            )
                        }
                        item {
                            if (hasMore) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedButton(onClick = onLoadMore, enabled = !isLoadingMore) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(if (isLoadingMore) "Loading…" else "Load more")
                                    }
                                }
                            }
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
        
        // Status Change Dialog
        showStatusDialog?.let { order ->
            AlertDialog(
                onDismissRequest = { showStatusDialog = null },
                title = { Text("Update Order Status") },
                text = {
                    Column {
                        Text("Order #${order.orderNumber}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Current status: ${order.status}")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OrderStatus.values().forEach { status ->
                            if (status != order.status) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onStatusChange(order.id, status)
                                            showStatusDialog = null
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = status.name.replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (status.ordinal > order.status.ordinal) {
                                        Text(
                                            text = "Mark as ${status.name.lowercase()}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showStatusDialog = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}

@Composable
fun AdminOrderItem(
    order: OrderUiModel,
    onStatusClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple list item design matching the images
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Customer name
            Text(
                text = order.customerName.ifBlank { "Customer" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Product and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.productName} • ${order.formattedDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Amount in green
                Text(
                    text = "₹ ${formatAmount(order.totalAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF4CAF50) // Green color
                )
            }
        }
        
        // Divider
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            thickness = 1.dp
        )
    }
}

@Composable
fun EmptyAdminOrdersView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ListAlt,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Orders Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "When customers place orders, they'll appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

// ViewModel removed; use AdminOrdersViewModel defined in its own file
