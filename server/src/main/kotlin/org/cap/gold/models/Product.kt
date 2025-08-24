package org.cap.gold.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

// Base table for common product fields
abstract class BaseProductTable(tableName: String) : UUIDTable(tableName) {
    val price = double("price")
    val weight = double("weight")
    val dimension = varchar("dimension", 100)
    val purity = varchar("purity", 50)
    val maxQuantity = integer("max_quantity")
    val category = varchar("category", 100)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

// Approved products table
object ProductsApproved : BaseProductTable("products_approved")

// Unapproved products table
object ProductsUnapproved : BaseProductTable("products_unapproved")

// Data class for product
// Common interface for both approved and unapproved products
interface Product {
    val id: UUID
    val price: Double
    val weight: Double
    val dimension: String
    val purity: String
    val maxQuantity: Int
    val category: String
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}

// Data classes for each product type
data class ApprovedProduct(
    override val id: UUID,
    override val price: Double,
    override val weight: Double,
    override val dimension: String,
    override val purity: String,
    override val maxQuantity: Int,
    override val category: String,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime
) : Product

data class UnapprovedProduct(
    override val id: UUID,
    override val price: Double,
    override val weight: Double,
    override val dimension: String,
    override val purity: String,
    override val maxQuantity: Int,
    override val category: String,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime
) : Product

// Extension functions to convert from ResultRow to data classes
fun ResultRow.toApprovedProduct() = ApprovedProduct(
    id = this[ProductsApproved.id].value,
    price = this[ProductsApproved.price],
    weight = this[ProductsApproved.weight],
    dimension = this[ProductsApproved.dimension],
    purity = this[ProductsApproved.purity],
    maxQuantity = this[ProductsApproved.maxQuantity],
    category = this[ProductsApproved.category],
    createdAt = this[ProductsApproved.createdAt],
    updatedAt = this[ProductsApproved.updatedAt]
)

fun ResultRow.toUnapprovedProduct() = UnapprovedProduct(
    id = this[ProductsUnapproved.id].value,
    price = this[ProductsUnapproved.price],
    weight = this[ProductsUnapproved.weight],
    dimension = this[ProductsUnapproved.dimension],
    purity = this[ProductsUnapproved.purity],
    maxQuantity = this[ProductsUnapproved.maxQuantity],
    category = this[ProductsUnapproved.category],
    createdAt = this[ProductsUnapproved.createdAt],
    updatedAt = this[ProductsUnapproved.updatedAt]
)
