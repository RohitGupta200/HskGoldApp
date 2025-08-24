package org.cap.gold.data.repository

import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.network.NetworkResponse

// NOTE: Renamed to avoid clashing with shared module's OrderRepository
interface AppOrderRepository {
    suspend fun getOrderById(orderId: String): Result<Order>
    
    suspend fun getUserOrders(
        page: Int = 0,
        pageSize: Int = 10,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null
    ): NetworkResponse<PaginatedResponse<Order>>
    
    suspend fun getAllOrders(
        page: Int = 0,
        pageSize: Int = 20,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null,
        query: String? = null
    ): NetworkResponse<PaginatedResponse<Order>>
    
    suspend fun createOrder(
        productId: String,
        quantity: Int,
        deliveryAddress: String,
        paymentMethod: String
    ): Result<Order>
    
    suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatus
    ): Result<Unit>
}

data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int
) {
    val hasNextPage: Boolean
        get() = (page + 1) * pageSize < total
    
    val isFirstPage: Boolean
        get() = page == 0
}

enum class OrderStatusGroup {
    ACTIVE,  // Pending orders
    PAST     // Confirmed or Cancelled orders
}
