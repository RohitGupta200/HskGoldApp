package org.cap.gold.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.ReferenceOption
import java.time.LocalDateTime
import java.util.*

// Order status enum
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

// Order status group
enum class OrderStatusGroup {
    ACTIVE,  // PENDING
    PAST     // CONFIRMED, CANCELLED
}

// Search order parameters
data class OrderSearchParams(
    val query: String? = null,
    val page: Int = 0,
    val pageSize: Int = 30,
    val status: OrderStatus? = null,
    val statusGroup: OrderStatusGroup? = null,
    val userId: UUID? = null
)

// Order table
object Orders : UUIDTable("orders") {
    val productId = uuid("product_id").references(ProductsApproved.id, onDelete = ReferenceOption.CASCADE)
    val productName = varchar("product_name", 255)
    val productPrice = double("product_price")
    val productWeight = double("product_weight")
    val productDimensions = varchar("product_dimensions", 100)
    val status = enumeration<OrderStatus>("status")
    val createdAt = long("created_at") // Using long for milliseconds since epoch
    val productQuantity = integer("product_quantity")
    val userMobile = varchar("user_mobile", 20)
    val userName = varchar("user_name", 255)
    val totalAmount = double("total_amount")
}

// Request DTO for creating an order
data class CreateOrderRequest(
    val productId: UUID,
    val productName: String,
    val productPrice: Double,
    val productWeight: Double,
    val productDimensions: String,
    val productQuantity: Int,
    val userMobile: String,
    val userName: String,
    val totalAmount: Double
)

// Data class for Order
data class Order(
    val id: UUID,
    val productId: UUID,
    val productName: String,
    val productPrice: Double,
    val productWeight: Double,
    val productDimensions: String,
    val status: OrderStatus,
    val createdAt: Long, // Changed to Long for milliseconds since epoch
    val productQuantity: Int,
    val userMobile: String,
    val userName: String,
    val totalAmount: Double
) {
    companion object {
        fun fromRow(row: ResultRow): Order = Order(
            id = row[Orders.id].value,
            productId = row[Orders.productId],
            productName = row[Orders.productName],
            productPrice = row[Orders.productPrice],
            productWeight = row[Orders.productWeight],
            productDimensions = row[Orders.productDimensions],
            status = row[Orders.status],
            createdAt = row[Orders.createdAt],
            productQuantity = row[Orders.productQuantity],
            userMobile = row[Orders.userMobile],
            userName = row[Orders.userName],
            totalAmount = row[Orders.totalAmount]
        )
    }
}

