package org.cap.gold.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

// Base table for common product fields
abstract class BaseProductTable(tableName: String) : UUIDTable(tableName) {
    val name = varchar("name", 200)
    val description = varchar("description", 1000)
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

// Product images table storing a single image per product (shared id across variants)
object ProductImages : Table("product_images") {
    val productId = uuid("product_id").uniqueIndex()
    val image = blob("image")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(productId)
}

// Data class for product
// Common interface for both approved and unapproved products
interface Product {
    val id: UUID
    val name: String
    val description: String
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
    override val name: String,
    override val description: String,
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
    override val name: String,
    override val description: String,
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
    name = this[ProductsApproved.name],
    description = this[ProductsApproved.description],
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
    name = this[ProductsUnapproved.name],
    description = this[ProductsUnapproved.description],
    price = this[ProductsUnapproved.price],
    weight = this[ProductsUnapproved.weight],
    dimension = this[ProductsUnapproved.dimension],
    purity = this[ProductsUnapproved.purity],
    maxQuantity = this[ProductsUnapproved.maxQuantity],
    category = this[ProductsUnapproved.category],
    createdAt = this[ProductsUnapproved.createdAt],
    updatedAt = this[ProductsUnapproved.updatedAt]
)
