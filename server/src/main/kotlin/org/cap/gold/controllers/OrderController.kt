package org.cap.gold.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.cap.gold.models.*
import org.cap.gold.repositories.OrderRepository
import java.util.*

class OrderController(private val orderRepository: OrderRepository) {

    // Serializable DTOs to avoid exposing UUIDs directly and to ensure proper JSON serialization
    @Serializable
    data class OrderResponse(
        val id: String,
        val productId: String,
        val productName: String,
        val productPrice: Double,
        val productWeight: Double,
        val productDimensions: String,
        val status: String,
        val createdAt: Long,
        val productQuantity: Int,
        val userMobile: String,
        val userName: String,
        val totalAmount: Double
    )

    @Serializable
    data class CreateOrderRequestDto(
        val productId: String,
        val productName: String,
        val productPrice: Double,
        val productWeight: Double,
        val productDimensions: String,
        val productQuantity: Int,
        val userMobile: String,
        val userName: String,
        val totalAmount: Double
    )

    @Serializable
    data class UpdateOrderRequestDto(
        val id: String,
        val productId: String,
        val productName: String,
        val productPrice: Double,
        val productWeight: Double,
        val productDimensions: String,
        val status: String,
        val productQuantity: Int,
        val userMobile: String,
        val userName: String,
        val totalAmount: Double
    )

    private fun Order.toDto() = OrderResponse(
        id = this.id.toString(),
        productId = this.productId.toString(),
        productName = this.productName,
        productPrice = this.productPrice,
        productWeight = this.productWeight,
        productDimensions = this.productDimensions,
        status = this.status.name,
        createdAt = this.createdAt,
        productQuantity = this.productQuantity,
        userMobile = this.userMobile,
        userName = this.userName,
        totalAmount = this.totalAmount
    )

    fun Route.orderRoutes() {
        route("/orders") {
            // Create a new order
            post {
                val req = call.receive<CreateOrderRequestDto>()
                val createdOrder = orderRepository.createOrder(
                    CreateOrderRequest(
                        productId = UUID.fromString(req.productId),
                        productName = req.productName,
                        productPrice = req.productPrice,
                        productWeight = req.productWeight,
                        productDimensions = req.productDimensions,
                        productQuantity = req.productQuantity,
                        userMobile = req.userMobile,
                        userName = req.userName,
                        totalAmount = req.totalAmount
                    )
                )
                call.respond(HttpStatusCode.Created, createdOrder.toDto())
            }

            // Get all orders (with optional status filter)
            get {
                val status = call.parameters["status"]?.let { OrderStatus.valueOf(it.uppercase()) }
                val (orders, _) = orderRepository.searchOrders(status = status)
                call.respond(orders.map { it.toDto() })
            }

            // Get order by ID
            get("{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid ID format")

                val order = orderRepository.getOrderById(id)
                if (order != null) {
                    call.respond(order.toDto())
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
                }
            }

            // Get orders by user mobile
            get("user/{mobile}") {
                val mobile = call.parameters["mobile"]
                    ?: throw IllegalArgumentException("Mobile number is required")

                val (orders, _) = orderRepository.getUserOrders(mobile)
                call.respond(orders.map { it.toDto() })
            }

            // Update order status
            put("{id}/status") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid ID format")

                val status = call.receive<Map<String, String>>()["status"]
                    ?.let { OrderStatus.valueOf(it.uppercase()) }
                    ?: throw IllegalArgumentException("Status is required")

                if (orderRepository.updateOrderStatus(id, status)) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Order status updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
                }
            }

            // Update order
            put("{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid ID format")

                val dto = call.receive<UpdateOrderRequestDto>()
                if (dto.id != id.toString()) {
                    throw IllegalArgumentException("Order ID in path and body do not match")
                }

                val order = Order(
                    id = id,
                    productId = UUID.fromString(dto.productId),
                    productName = dto.productName,
                    productPrice = dto.productPrice,
                    productWeight = dto.productWeight,
                    productDimensions = dto.productDimensions,
                    status = OrderStatus.valueOf(dto.status.uppercase()),
                    createdAt = System.currentTimeMillis(), // keep existing createdAt if needed via DB fetch
                    productQuantity = dto.productQuantity,
                    userMobile = dto.userMobile,
                    userName = dto.userName,
                    totalAmount = dto.totalAmount
                )

                if (orderRepository.updateOrder(order)) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Order updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
                }
            }

            // Delete order
            delete("{id}") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid ID format")

                if (orderRepository.deleteOrder(id)) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Order deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
                }
            }

            // Get order statistics
            get("statistics") {
                val (allOrders, _) = orderRepository.searchOrders()
                val totalOrders = allOrders.size
                val pendingOrders = orderRepository.getOrderCountByStatus(OrderStatus.PENDING)
                val deliveredOrders = orderRepository.getOrderCountByStatus(OrderStatus.CONFIRMED)
                val totalSales = orderRepository.getTotalSales()

                call.respond(
                    mapOf(
                        "totalOrders" to totalOrders,
                        "pendingOrders" to pendingOrders,
                        "deliveredOrders" to deliveredOrders,
                        "totalSales" to totalSales
                    )
                )
            }
        }
    }
}

