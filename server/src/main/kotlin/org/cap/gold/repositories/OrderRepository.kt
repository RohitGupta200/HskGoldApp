package org.cap.gold.repositories

import org.cap.gold.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.util.*

class OrderRepository {
    
    // Create
    suspend fun createOrder(createOrderRequest: CreateOrderRequest): Order = transaction {
        val id = UUID.randomUUID()
        Orders.insert {
            it[this.id] = id
            it[productId] = createOrderRequest.productId
            it[productName] = createOrderRequest.productName
            it[productPrice] = createOrderRequest.productPrice
            it[productWeight] = createOrderRequest.productWeight
            it[productDimensions] = createOrderRequest.productDimensions
            it[status] = OrderStatus.PENDING
            it[createdAt] = System.currentTimeMillis()
            it[productQuantity] = createOrderRequest.productQuantity
            it[userMobile] = createOrderRequest.userMobile
            it[userName] = createOrderRequest.userName
            it[totalAmount] = createOrderRequest.totalAmount
        }
        
        // Query directly within the same transaction
        Orders.select { Orders.id eq id }
            .map { Order.fromRow(it) }
            .single()
    }
    
    // Read
    suspend fun getOrderById(id: UUID): Order? = transaction {
        Orders.select { Orders.id eq id }
            .map { Order.fromRow(it) }
            .singleOrNull()
    }

    suspend fun getUserOrdersWithoutPagination(userMobile: String): Pair<List<Order>, Long> = transaction {
        val query = Orders.select { Orders.userMobile eq userMobile }
        val total = query.count()
        val orders=query.map { Order.fromRow(it) }
        return@transaction orders to total

    }
    
    suspend fun searchOrders(
        query: String? = null,
        page: Int = 0,
        pageSize: Int = 30,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null
    ): Pair<List<Order>, Long> = transaction {
        val safePage = if (page < 0) 0 else page
        val safePageSize = when {
            pageSize <= 0 -> 30
            pageSize > 500 -> 500
            else -> pageSize
        }

        var queryBuilder = when (statusGroup) {
            OrderStatusGroup.ACTIVE -> Orders.select { Orders.status eq OrderStatus.PENDING }
            OrderStatusGroup.PAST -> Orders.select {
                (Orders.status eq OrderStatus.CONFIRMED) or (Orders.status eq OrderStatus.CANCELLED)
            }
            null -> Orders.selectAll()
        }

        // Apply search query if provided and not blank
        val trimmed = query?.trim()
        if (!trimmed.isNullOrEmpty()) {
            queryBuilder = queryBuilder.andWhere {
                Orders.productName.like("%$trimmed%") or
                Orders.userMobile.like("%$trimmed%") or
                Orders.userName.like("%$trimmed%")
            }
        }

        // Apply status filter if provided
        status?.let { orderStatus ->
            queryBuilder = queryBuilder.andWhere { Orders.status eq orderStatus }
        }

        val total = queryBuilder.count()

        val orders = queryBuilder
            .orderBy(Orders.createdAt to SortOrder.DESC)
            .limit(safePageSize, (safePage * safePageSize).toLong())
            .map { Order.fromRow(it) }

        return@transaction orders to total
    }
    
    suspend fun getUserOrders(
        userMobile: String,
        page: Int = 0,
        pageSize: Int = 150,
        status: OrderStatus? = null,
        statusGroup: OrderStatusGroup? = null
    ): Pair<List<Order>, Long> = transaction {
        val safePage = if (page < 0) 0 else page
        val safePageSize = when {
            pageSize <= 0 -> 30
            pageSize > 500 -> 500
            else -> pageSize
        }
        var queryBuilder = Orders.select { Orders.userMobile eq userMobile }
        
        // Apply status group filter
        queryBuilder = when (statusGroup) {
            OrderStatusGroup.ACTIVE -> queryBuilder.andWhere { Orders.status eq OrderStatus.PENDING }
            OrderStatusGroup.PAST -> queryBuilder.andWhere { 
                (Orders.status eq OrderStatus.CONFIRMED) or (Orders.status eq OrderStatus.CANCELLED) 
            }
            null -> queryBuilder
        }
        
        // Apply status filter if provided
        status?.let { orderStatus ->
            queryBuilder = queryBuilder.andWhere { Orders.status eq orderStatus }
        }
        
        val total = queryBuilder.count()
        
        val orders = queryBuilder
            .orderBy(Orders.createdAt to SortOrder.DESC)
            .limit(pageSize, (page * pageSize).toLong())
            .map { Order.fromRow(it) }
            
        return@transaction orders to total
    }
    
    // Update
    suspend fun updateOrderStatus(id: UUID, status: OrderStatus): Boolean = transaction {
        Orders.update({ Orders.id eq id }) {
            it[Orders.status] = status
        } > 0
    }
    
    suspend fun updateOrder(order: Order): Boolean = transaction {
        Orders.update({ Orders.id eq order.id }) {
            it[productId] = order.productId
            it[productName] = order.productName
            it[productPrice] = order.productPrice
            it[productWeight] = order.productWeight
            it[productDimensions] = order.productDimensions
            it[status] = order.status
            it[productQuantity] = order.productQuantity
            it[userMobile] = order.userMobile
            it[userName] = order.userName
            it[totalAmount] = order.totalAmount
        } > 0
    }
    
    // Delete
    suspend fun deleteOrder(id: UUID): Boolean = transaction {
        Orders.deleteWhere { Orders.id eq id } > 0
    }
    
    // Additional helper methods
    suspend fun getOrderCountByStatus(status: OrderStatus): Int = transaction {
        Orders.select { Orders.status eq status }.count().toInt()
    }
    
    suspend fun getTotalSales(): Double = transaction {
        Orders.slice(Orders.totalAmount.sum())
            .select { Orders.status eq OrderStatus.CONFIRMED }
            .map { it[Orders.totalAmount.sum()] ?: 0.0 }
            .first()
    }

    // Maintenance: delete orders older than the given cutoff (epoch millis)
    suspend fun deleteOrdersOlderThan(cutoffMillis: Long): Int = transaction {
        Orders.deleteWhere { Orders.createdAt less cutoffMillis }
    }
}

