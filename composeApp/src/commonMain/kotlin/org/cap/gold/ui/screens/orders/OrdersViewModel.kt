package org.cap.gold.ui.screens.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.repository.AppOrderRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrdersViewModel : KoinComponent {
    private val orderRepository: AppOrderRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    var uiState: OrdersUiState by mutableStateOf(OrdersUiState.Loading)
        private set
    
    private fun formatDate(millis: Long): String {
        return try {
            val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
            val month = dt.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)
            val day = dt.dayOfMonth
            val year = dt.year
            "$month ${day.toString().padStart(2, '0')}, $year"
        } catch (e: Exception) { "" }
    }
    
    fun loadOrders() {
        uiState = OrdersUiState.Loading
        scope.launch {
            try {
                when (val resp = orderRepository.getUserOrders()) {
                    is org.cap.gold.data.network.NetworkResponse.Success -> {
                        val orders = resp.data.data.map { order ->
                            val dateMillis = parseDateMillis(order.createdAt)
                            OrderUiModel(
                                id = order.id,
                                orderNumber = order.id, // fallback
                                productName = order.productId, // fallback until product lookup
                                quantity = order.quantity,
                                totalAmount = order.totalPrice,
                                status = order.status,
                                date = dateMillis,
                                formattedDate = if (dateMillis > 0L) formatDate(dateMillis) else "",
                                productImageUrl = null,
                                customerName = order.name
                            )
                        }
                        uiState = OrdersUiState.Success(orders)
                    }
                    is org.cap.gold.data.network.NetworkResponse.Error -> {
                        uiState = OrdersUiState.Error(resp.message ?: "Failed to load orders")
                    }
                    is org.cap.gold.data.network.NetworkResponse.Loading -> {
                        uiState = OrdersUiState.Loading
                    }
                }
            } catch (e: Exception) {
                uiState = OrdersUiState.Error(e.message ?: "Failed to load orders")
            }
        }
    }
    
    fun refresh() {
        loadOrders()
    }
    
    private fun parseDateMillis(value: String): Long = value.toLongOrNull() ?: 0L
}
