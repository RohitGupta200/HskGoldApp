package org.cap.gold.ui.screens.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.repository.AppOrderRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrderDetailViewModel : KoinComponent {
    private val orderRepository: AppOrderRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = orderRepository.getOrderById(orderId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    order = result.getOrNull(),
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load order"
                )
            }
        }
    }

    fun updateOrderStatus(newStatus: OrderStatus) {
        _uiState.value = _uiState.value.copy(
            showStatusUpdateDialog = true,
            newStatus = newStatus
        )
    }

    fun confirmStatusUpdate() {
        val orderId = _uiState.value.order?.id ?: return
        val newStatus = _uiState.value.newStatus ?: return
        
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = orderRepository.updateOrderStatus(orderId, newStatus)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    order = _uiState.value.order?.copy(status = newStatus),
                    showStatusUpdateDialog = false,
                    message = "Order status updated successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showStatusUpdateDialog = false,
                    message = result.exceptionOrNull()?.message ?: "Failed to update order status"
                )
            }
        }
    }

    fun dismissStatusUpdateDialog() {
        _uiState.value = _uiState.value.copy(
            showStatusUpdateDialog = false,
            newStatus = null
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showStatusUpdateDialog: Boolean = false,
    val newStatus: OrderStatus? = null,
    val message: String? = null
)
