package org.cap.gold.ui.screens.orders

import org.cap.gold.data.model.OrderStatus

/**
 * UI Model for displaying order information in the list
 */
data class OrderUiModel(
    val id: String,
    val orderNumber: String,
    val productName: String,
    val quantity: Int,
    val totalAmount: Double,
    val status: OrderStatus,
    val date: Long,
    val formattedDate: String,
    val productImageUrl: String? = null,
    val customerName: String = ""
)

/**
 * UI State for the Orders screen
 */
sealed class OrdersUiState {
    object Loading : OrdersUiState()
    data class Success(val orders: List<OrderUiModel>) : OrdersUiState()
    data class Error(val message: String) : OrdersUiState()
}
