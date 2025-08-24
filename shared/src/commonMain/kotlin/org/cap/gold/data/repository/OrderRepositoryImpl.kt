package org.cap.gold.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import org.cap.gold.api.ApiClient
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderResponse
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.util.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OrderRepositoryImpl : OrderRepository, KoinComponent {
    private val client: HttpClient by inject()

    override suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val httpResp: HttpResponse = client.get("/api/orders/$orderId")
            if (httpResp.status.value !in 200..299) {
                val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to fetch order" }
                return Result.Error(msg.ifBlank { "Failed to fetch order" })
            }
            // Try canonical DTO first
            try {
                val response: OrderResponse = httpResp.body()
                if (response.success && response.order != null) Result.Success(response.order)
                else Result.Error(response.message ?: "Failed to fetch order")
            } catch (_: Exception) {
                // Fallback: server may return Order directly
                val order: Order = httpResp.body()
                Result.Success(order)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error occurred")
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order> {
        return try {
            val httpResp: HttpResponse = client.patch("/api/orders/$orderId/status") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("status" to status.name))
            }
            if (httpResp.status.value !in 200..299) {
                val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to update order status" }
                return Result.Error(msg.ifBlank { "Failed to update order status" })
            }
            try {
                val response: OrderResponse = httpResp.body()
                if (response.success && response.order != null) Result.Success(response.order)
                else Result.Error(response.message ?: "Failed to update order status")
            } catch (_: Exception) {
                val order: Order = httpResp.body()
                Result.Success(order)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error occurred")
        }
    }

    override suspend fun getOrdersByStatus(status: OrderStatus?): Result<List<Order>> {
        return try {
            val httpResp: HttpResponse = client.get("/api/orders") {
                status?.let { parameter("status", it.name) }
            }
            if (httpResp.status.value !in 200..299) {
                val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to fetch orders" }
                return Result.Error(msg.ifBlank { "Failed to fetch orders" })
            }
            // Try typed list
            try {
                val list: List<Order> = httpResp.body()
                Result.Success(list)
            } catch (_: Exception) {
                // Try wrapped structure { orders: [...] }
                @kotlinx.serialization.Serializable
                data class Wrapped<T>(val orders: List<T>? = null)
                try {
                    val wrapped: Wrapped<Order> = httpResp.body()
                    Result.Success(wrapped.orders ?: emptyList())
                } catch (_: Exception) {
                    // Fallback: parse text and error out
                    val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to fetch orders" }
                    Result.Error(msg.ifBlank { "Failed to fetch orders" })
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch orders")
        }
    }
}
