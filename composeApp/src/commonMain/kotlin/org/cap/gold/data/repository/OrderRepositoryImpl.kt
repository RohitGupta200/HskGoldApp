package org.cap.gold.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.Serializable
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.network.NetworkResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// NOTE: Renamed to avoid clashing with shared module's OrderRepositoryImpl
class AppOrderRepositoryImpl : AppOrderRepository, KoinComponent {
    private val client: HttpClient by inject()

    override suspend fun getOrderById(orderId: String): Result<Order> = try {
        val order: Order = client.get("/api/orders/$orderId").body()
        Result.success(order)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserOrders(
        page: Int,
        pageSize: Int,
        status: OrderStatus?,
        statusGroup: OrderStatusGroup?
    ): NetworkResponse<PaginatedResponse<Order>> {
        return try {
            val response = client.get("/orders/me") {
                parameter("page", page)
                parameter("pageSize", pageSize)
                status?.let { parameter("status", it.name) }
                statusGroup?.let { parameter("statusGroup", it.name) }
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val paginatedResponse = response.body<PaginatedResponseDto<Order>>()
                    NetworkResponse.Success(
                        PaginatedResponse(
                            data = paginatedResponse.data,
                            total = paginatedResponse.total,
                            page = paginatedResponse.page,
                            pageSize = paginatedResponse.pageSize
                        )
                    )
                }
                else -> NetworkResponse.Error("Failed to fetch orders")
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun getAllOrders(
        page: Int,
        pageSize: Int,
        status: OrderStatus?,
        statusGroup: OrderStatusGroup?,
        query: String?
    ): NetworkResponse<PaginatedResponse<Order>> {
        return try {
            val response = client.get("/api/orders") {
                parameter("page", page)
                parameter("pageSize", pageSize)
                status?.let { parameter("status", it.name) }
                statusGroup?.let { parameter("statusGroup", it.name) }
                query?.takeIf { it.isNotBlank() }?.let { parameter("query", it) }
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    try {
                        val paginatedResponse = response.body<PaginatedResponseDto<Order>>()
                        NetworkResponse.Success(
                            PaginatedResponse(
                                data = paginatedResponse.data,
                                total = paginatedResponse.total,
                                page = paginatedResponse.page,
                                pageSize = paginatedResponse.pageSize
                            )
                        )
                    } catch (e: JsonConvertException) {
                        // Server may return a plain list [] instead of paginated object
                        val list = response.body<List<Order>>()
                        NetworkResponse.Success(
                            PaginatedResponse(
                                data = list,
                                total = list.size.toLong(),
                                page = page,
                                pageSize = pageSize
                            )
                        )
                    }
                }
                HttpStatusCode.Unauthorized -> NetworkResponse.Error("Unauthorized")
                HttpStatusCode.Forbidden -> NetworkResponse.Error("Access denied")
                else -> NetworkResponse.Error("Failed to fetch orders")
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun createOrder(
        productId: String,
        quantity: Int,
        deliveryAddress: String,
        paymentMethod: String
    ): Result<Order> = try {
        val created: Order = client.post("/api/orders") {
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(productId, quantity, deliveryAddress, paymentMethod))
        }.body()
        Result.success(created)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = try {
        client.put("/api/orders/$orderId/status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status))
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    @Serializable
    private data class CreateOrderRequest(
        val productId: String,
        val quantity: Int,
        val deliveryAddress: String,
        val paymentMethod: String
    )

    @Serializable
    private data class UpdateOrderStatusRequest(
        val status: OrderStatus
    )

    @Serializable
    private data class PaginatedResponseDto<T>(
        val data: List<T>,
        val total: Long,
        val page: Int,
        val pageSize: Int
    )
}
