package org.cap.gold.data.repository

import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.util.Result

interface OrderRepository {
    suspend fun getOrderById(orderId: String): Result<Order>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order>
    suspend fun getOrdersByStatus(status: OrderStatus? = null): Result<List<Order>>
}
