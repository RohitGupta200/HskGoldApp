package org.cap.gold.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.cap.gold.util.newUUID

@Serializable
data class Order(
    val id: String = newUUID(),
    val userId: String = "",
    val productId: String = "",
    @SerialName("productQuantity") val quantity: Int = 1,
    @SerialName("totalAmount") val totalPrice: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    // Server sends epoch millis as number
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val address: String = "",
    @SerialName("userMobile") val phoneNumber: String = "",
    @SerialName("userName") val name: String = "",
    @SerialName("productName") val productName: String = ""
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
