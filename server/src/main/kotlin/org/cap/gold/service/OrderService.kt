package org.cap.gold.service

import org.cap.gold.dao.OrderDao
import org.cap.gold.models.*
import java.util.*

/**
 * Service layer for order-related business logic
 */
class OrderService(private val orderDao: OrderDao) {
    
    /**
     * Search orders with pagination and filtering
     * @return Pair of orders list and total count
     */
    fun searchOrders(
        query: String? = null,
        page: Int = 0,
        pageSize: Int = 30,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null,
        userId: UUID? = null
    ): Pair<List<Order>, Long> {
        require(page >= 0) { "Page must be non-negative" }
        require(pageSize > 0) { "Page size must be positive" }
        
        // Validate that either status or statusGroup is provided, not both
        if (status != null && statusGroup != null) {
            throw IllegalArgumentException("Cannot specify both status and statusGroup")
        }

        val params = OrderSearchParams(
            query = query,
            page = page,
            pageSize = pageSize,
            status = status,
            statusGroup = statusGroup,
            userId = userId
        )
        
        return orderDao.searchOrders(params)
    }

    /**
     * Get orders by user with pagination and filtering
     * @return Pair of orders list and total count
     */
    fun getOrdersByUser(
        userMobile: String,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null,
        page: Int = 0,
        pageSize: Int = 30
    ): Pair<List<Order>, Long> {
        require(page >= 0) { "Page must be non-negative" }
        require(pageSize > 0) { "Page size must be positive" }
        require(userMobile.isNotBlank()) { "User mobile is required" }
        
        // Validate that either status or statusGroup is provided, not both
        if (status != null && statusGroup != null) {
            throw IllegalArgumentException("Cannot specify both status and statusGroup")
        }

        return orderDao.getOrdersByUser(
            userMobile = userMobile,
            status = status,
            statusGroup = statusGroup,
            page = page,
            pageSize = pageSize
        )
    }

    /**
     * Create a new order
     * @throws IllegalArgumentException if the request is invalid
     */
    fun createOrder(createRequest: CreateOrderRequest): Order {
        // Validate the request
        require(createRequest.productQuantity > 0) { "Product quantity must be greater than 0" }
        require(createRequest.totalAmount > 0) { "Total amount must be greater than 0" }
        require(createRequest.productName.isNotBlank()) { "Product name is required" }
        require(createRequest.userMobile.isNotBlank()) { "User mobile is required" }
        require(createRequest.productId != UUID(0, 0)) { "Valid product ID is required" }
        require(createRequest.productPrice >= 0) { "Product price must be non-negative" }
        require(createRequest.productWeight >= 0) { "Product weight must be non-negative" }
        require(createRequest.productDimensions.isNotBlank()) { "Product dimensions are required" }

        return orderDao.createOrder(createRequest)
    }

    /**
     * Update order status
     * @return true if the update was successful, false otherwise
     * @throws IllegalArgumentException if the order ID is invalid
     */
    fun updateOrderStatus(orderId: UUID, status: OrderStatus): Boolean {
        require(orderId != UUID(0, 0)) { "Valid order ID is required" }
        return orderDao.updateOrderStatus(orderId, status)
    }

    /**
     * Get order by ID
     * @return Order if found, null otherwise
     * @throws IllegalArgumentException if the order ID is invalid
     */
    fun getOrderById(orderId: UUID): Order? {
        require(orderId != UUID(0, 0)) { "Valid order ID is required" }
        return orderDao.getOrderById(orderId)
    }
}
