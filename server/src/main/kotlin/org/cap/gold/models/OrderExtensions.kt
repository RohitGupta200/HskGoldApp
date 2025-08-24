package org.cap.gold.models

import org.jetbrains.exposed.sql.ResultRow
import java.util.*

/**
 * Extension function to convert a ResultRow to an Order
 */
fun ResultRow.toOrder(): Order = Order.fromRow(this)

/**
 * Extension function to convert Order to a map for API response
 */
fun Order.toResponseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "productId" to productId,
    "productName" to productName,
    "productPrice" to productPrice,
    "productWeight" to productWeight,
    "productDimensions" to productDimensions,
    "status" to status.name,
    "createdAt" to createdAt,
    "productQuantity" to productQuantity,
    "userMobile" to userMobile,
    "userName" to userName,
    "totalAmount" to totalAmount
)

/**
 * Extension function to convert CreateOrderRequest to Order
 */
fun CreateOrderRequest.toOrder(): Order = Order(
    id = UUID.randomUUID(),
    productId = productId,
    productName = productName,
    productPrice = productPrice,
    productWeight = productWeight,
    productDimensions = productDimensions,
    status = OrderStatus.PENDING,
    createdAt = System.currentTimeMillis(),
    productQuantity = productQuantity,
    userMobile = userMobile,
    userName = userName,
    totalAmount = totalAmount
)
