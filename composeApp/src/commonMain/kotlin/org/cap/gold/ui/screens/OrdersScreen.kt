package org.cap.gold.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.computeHorizontalBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.ui.screens.orders.OrderUiModel
import org.cap.gold.ui.screens.orders.OrdersUiState
import org.cap.gold.ui.screens.orders.OrdersViewModel
import org.cap.gold.util.formatAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.IO
import org.koin.compose.koinInject
import org.cap.gold.data.remote.ProductApiService
 
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
    val listState = rememberLazyListState()
    val apiService = koinInject<ProductApiService>()
    val imageBytesMap = remember { mutableStateMapOf<String, ByteArray>() } // productId -> bytes
    val imageSemaphore = remember { Semaphore(3) }
    val inFlight = remember { mutableSetOf<String>() } // productId
    val scope = rememberCoroutineScope()
    
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Orders") },
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
                        uiState.orders.filter { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.SHIPPED || it.status == OrderStatus.PARTIAL_COMPLETED }
                    } else {
                        // Past: Delivered, Cancelled,Completed
                        uiState.orders.filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED || it.status == OrderStatus.COMPLETED }
                    }

                    // Lazily fetch visible product images (similar to ProductsScreen)
                    LaunchedEffect(listState, orders) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String } }
                            .collectLatest { visibleOrderIds ->
                                val orderById = orders.associateBy { it.id }
                                val visibleProductIds = visibleOrderIds.mapNotNull { id -> orderById[id]?.productId }
                                val missingVisible = visibleProductIds.filter { pid -> imageBytesMap[pid] == null && !inFlight.contains(pid) }
                                missingVisible.forEach { pid ->
                                    // mark in-flight on main
                                    inFlight.add(pid)
                                    scope.launch(Dispatchers.IO) {
                                        imageSemaphore.withPermit {
                                            val bytes = runCatching { apiService.getProductImage(pid) }.getOrNull()
                                            withContext(Dispatchers.Main) {
                                                if (bytes != null) {
                                                    imageBytesMap[pid] = bytes
                                                }
                                                inFlight.remove(pid)
                                            }
                                        }
                                    }
                                }
                            }
                    }
                    
                    if (orders.isEmpty()) {
                        EmptyOrdersView(selectedTabIndex == 0)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            state = listState
                        ) {
                            items(orders, key = { it.id }) { order ->
                                OrderItem(
                                    order = order,
                                    imageBytes = imageBytesMap[order.productId],
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
    imageBytes: ByteArray? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Simplified list row for regular users
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: product image
        val painter = rememberAsyncImagePainter(model = imageBytes)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEDEDED))
        ) {
            if (imageBytes != null) {
                Image(
                    painter = painter,
                    contentDescription = order.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle: name and quantity
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = order.productName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (order.quantity == 1) "1 item" else "${order.quantity} items" + "• ${order.formattedDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(.6f), horizontalAlignment = Alignment.End) {
            // Right: total amount
            Text(
                text = "₹ ${formatAmount(order.totalAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            val amountColor = when (order.status) {
                OrderStatus.CONFIRMED -> Color(0xFF4CAF50)
                OrderStatus.PENDING -> Color(0xFFE19E04)
                OrderStatus.CANCELLED -> Color(0xFFF44336)
                OrderStatus.SHIPPED -> Color(0xFF1E88E5)
                OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                OrderStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                OrderStatus.PARTIAL_COMPLETED -> Color(0xFFFF6D00)
            }
            val text = when (order.status) {
                OrderStatus.CONFIRMED -> "Accepted"
                OrderStatus.PENDING -> "Pending"
                OrderStatus.CANCELLED -> "Rejected"
                OrderStatus.SHIPPED -> "Shipped"
                OrderStatus.DELIVERED -> "Delivered"
                OrderStatus.COMPLETED -> "Completed"
                OrderStatus.PARTIAL_COMPLETED -> "Partially Completed"
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = amountColor
            )
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
