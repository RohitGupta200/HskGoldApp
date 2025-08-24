package org.cap.gold.dao

import org.cap.gold.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OrderDao {
    /**
     * Search orders with pagination and filtering
     */
    fun searchOrders(params: OrderSearchParams): Pair<List<Order>, Long> = transaction {
        var query = when (params.status) {
            null -> when (params.statusGroup) {
                OrderStatusGroup.ACTIVE -> Orders.select { Orders.status eq OrderStatus.PENDING }
                OrderStatusGroup.PAST -> {
                    Orders.select { (Orders.status eq OrderStatus.CONFIRMED) or (Orders.status eq OrderStatus.CANCELLED) }
                }
                null -> Orders.selectAll()
            }
            else -> Orders.select { Orders.status eq params.status }
        }

        // Apply search query if provided
        params.query?.takeIf { it.isNotBlank() }?.let { searchQuery ->
            query = query.andWhere {
                (Orders.productName like "%$searchQuery%") or
                (Orders.userMobile like "%$searchQuery%") or
                (Orders.userName like "%$searchQuery%")
            }
        }

        // Apply user filter if provided
        params.userId?.let { userId ->
            // Note: We don't have userId in Orders table, so we'll need to join with users table
            // For now, we'll skip this filter as we don't have the users table structure
        }

        val totalCount = query.count()
        val orders = query
            .orderBy(Orders.createdAt to SortOrder.DESC)
            .limit(params.pageSize, (params.page * params.pageSize).toLong())
            .map { Order.fromRow(it) }

        return@transaction orders to totalCount
    }

    /**
     * Get orders by user with pagination and filtering
     */
    fun getOrdersByUser(
        userMobile: String,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null,
        page: Int = 0,
        pageSize: Int = 30
    ): Pair<List<Order>, Long> = transaction {
        var query = Orders.select { Orders.userMobile eq userMobile }

        // Apply status filter if provided
        status?.let {
            query = query.andWhere { Orders.status eq it }
        } ?: statusGroup?.let {
            when (it) {
                OrderStatusGroup.ACTIVE -> query = query.andWhere { Orders.status eq OrderStatus.PENDING }
                OrderStatusGroup.PAST -> query = query.andWhere {
                    (Orders.status eq OrderStatus.CONFIRMED) or (Orders.status eq OrderStatus.CANCELLED)
                }
            }
        }

        val totalCount = query.count()
        val orders = query
            .orderBy(Orders.createdAt to SortOrder.DESC)
            .limit(pageSize, (page * pageSize).toLong())
            .map { Order.fromRow(it) }

        return@transaction orders to totalCount
    }

    /**
     * Create a new order
     */
    fun createOrder(createRequest: CreateOrderRequest): Order = transaction {
        val orderId = UUID.randomUUID()
        val now = System.currentTimeMillis()

        Orders.insert {
            it[id] = orderId
            it[productId] = createRequest.productId
            it[productName] = createRequest.productName
            it[productPrice] = createRequest.productPrice
            it[productWeight] = createRequest.productWeight
            it[productDimensions] = createRequest.productDimensions
            it[status] = OrderStatus.PENDING
            it[createdAt] = now
            it[productQuantity] = createRequest.productQuantity
            it[userMobile] = createRequest.userMobile
            it[userName] = createRequest.userName
            it[totalAmount] = createRequest.totalAmount
        }

        // Return the created order by fetching it back from the database
        Order.fromRow(Orders.select { Orders.id eq orderId }.single())
    }

    /**
     * Update order status
     * @return true if the update was successful, false otherwise
     */
    fun updateOrderStatus(orderId: UUID, status: OrderStatus): Boolean = transaction {
        val updatedRows = Orders.update({ Orders.id eq orderId }) {
            it[Orders.status] = status
        }
        updatedRows > 0
    }

    /**
     * Get order by ID
     * @return Order if found, null otherwise
     */
    fun getOrderById(orderId: UUID): Order? = transaction {
        Orders.select { Orders.id eq orderId }
            .map { Order.fromRow(it) }
            .singleOrNull()
    }
}
