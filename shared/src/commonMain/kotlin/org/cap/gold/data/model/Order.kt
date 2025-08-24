package org.cap.gold.data.model

import kotlinx.serialization.Serializable
import org.cap.gold.util.newUUID

@Serializable
data class Order(
    val id: String = newUUID(),
    val userId: String = "",
    val productId: String = "",
    val quantity: Int = 1,
    val totalPrice: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: String = "",
    val updatedAt: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val name: String = ""
)

@Serializable
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

// Request/Response DTOs
@Serializable
data class CreateOrderRequest(
    val productId: String,
    val quantity: Int,
    val address: String,
    val phoneNumber: String,
    val name: String
)

@Serializable
data class OrderResponse(
    val success: Boolean,
    val message: String,
    val order: Order? = null
)
