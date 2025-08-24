package org.cap.gold.ui.screens.admin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.repository.AppOrderRepository
import org.cap.gold.ui.screens.orders.OrderUiModel
import org.cap.gold.ui.screens.orders.OrdersUiState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AdminOrdersViewModel : KoinComponent {
    private val orderRepository: AppOrderRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var currentQuery: String? = null

    private fun formatDateTime(millis: Long): String {
        return try {
            val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
            val month = dt.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)
            val day = dt.dayOfMonth.toString().padStart(2, '0')
            val year = dt.year
            val hour12 = ((dt.hour % 12).let { if (it == 0) 12 else it }).toString().padStart(2, '0')
            val minute = dt.minute.toString().padStart(2, '0')
            val ampm = if (dt.hour < 12) "AM" else "PM"
            "$month $day, $year • $hour12:$minute $ampm"
        } catch (e: Exception) { "" }
    }

    init { loadOrders(true) }

    fun loadOrders(reset: Boolean = false) {
        if (reset) {
            _uiState.value = OrdersUiState.Loading
            currentPage = 0
            _hasMore.value = false
        }
        scope.launch {
            try {
                when (val resp = orderRepository.getAllOrders(
                    page = currentPage,
                    pageSize = pageSize,
                    status = null,
                    statusGroup = null,
                    query = currentQuery
                )) {
                    is org.cap.gold.data.network.NetworkResponse.Success -> {
                        val mapped = resp.data.data.map { order ->
                            val dateMillis = order.createdAt.toLongOrNull() ?: 0L
                            OrderUiModel(
                                id = order.id,
                                orderNumber = order.id,
                                productName = order.productId,
                                quantity = order.quantity,
                                totalAmount = order.totalPrice,
                                status = order.status,
                                date = dateMillis,
                                formattedDate = if (dateMillis > 0L) formatDateTime(dateMillis) else "",
                                productImageUrl = null,
                                customerName = order.name
                            )
                        }.sortedByDescending { it.date }
                        val current = (_uiState.value as? OrdersUiState.Success)?.orders ?: emptyList()
                        val combined = if (reset || currentPage == 0) mapped else current + mapped
                        _uiState.value = OrdersUiState.Success(combined)
                        _hasMore.value = resp.data.hasNextPage
                    }
                    is org.cap.gold.data.network.NetworkResponse.Error -> {
                        _uiState.value = OrdersUiState.Error(resp.message ?: "Failed to load orders")
                    }
                    is org.cap.gold.data.network.NetworkResponse.Loading -> {
                        _uiState.value = OrdersUiState.Loading
                    }
                }
            } catch (e: Exception) {
                _uiState.value = OrdersUiState.Error(e.message ?: "Failed to load orders")
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value) return
        _isLoadingMore.value = true
        currentPage += 1
        scope.launch {
            loadOrders(reset = false)
            _isLoadingMore.value = false
        }
    }

    fun search(query: String) {
        currentQuery = query.ifBlank { null }
        currentPage = 0
        loadOrders(reset = true)
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        scope.launch {
            try {
                // Ignore result content, just refresh on success or continue regardless
                orderRepository.updateOrderStatus(orderId, newStatus)
                loadOrders(reset = true)
            } catch (e: Exception) {
                println("Failed to update order status: ${e.message}")
            }
        }
    }
}
