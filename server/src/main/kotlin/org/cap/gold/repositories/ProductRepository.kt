package org.cap.gold.repositories

import org.cap.gold.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProductRepository {
    
    // Create
    suspend fun createApprovedProduct(product: ApprovedProduct): ApprovedProduct = transaction {
        val id = ProductsApproved.insertAndGetId {
            it[price] = product.price
            it[weight] = product.weight
            it[dimension] = product.dimension
            it[purity] = product.purity
            it[maxQuantity] = product.maxQuantity
            it[category] = product.category
        }.value
        product.copy(id = id)
    }
    
    suspend fun createUnapprovedProduct(product: UnapprovedProduct): UnapprovedProduct = transaction {
        val id = ProductsUnapproved.insertAndGetId {
            it[price] = product.price
            it[weight] = product.weight
            it[dimension] = product.dimension
            it[purity] = product.purity
            it[maxQuantity] = product.maxQuantity
            it[category] = product.category
        }.value
        
        product.copy(id = id)
    }

    // Create with a specified UUID (used to ensure both tables share same id)
    suspend fun insertApprovedWithId(id: UUID, product: ApprovedProduct): ApprovedProduct = transaction {
        ProductsApproved.insert {
            it[ProductsApproved.id] = id
            it[price] = product.price
            it[weight] = product.weight
            it[dimension] = product.dimension
            it[purity] = product.purity
            it[maxQuantity] = product.maxQuantity
            it[category] = product.category
            it[createdAt] = product.createdAt
            it[updatedAt] = product.updatedAt
        }
        product.copy(id = id)
    }

    suspend fun insertUnapprovedWithId(id: UUID, product: UnapprovedProduct): UnapprovedProduct = transaction {
        ProductsUnapproved.insert {
            it[ProductsUnapproved.id] = id
            it[price] = product.price
            it[weight] = product.weight
            it[dimension] = product.dimension
            it[purity] = product.purity
            it[maxQuantity] = product.maxQuantity
            it[category] = product.category
            it[createdAt] = product.createdAt
            it[updatedAt] = product.updatedAt
        }
        product.copy(id = id)
    }
    
    suspend fun approveProduct(productId: UUID): ApprovedProduct? = transaction {
        // Get the unapproved product
        val unapproved = ProductsUnapproved.select { ProductsUnapproved.id eq productId }
            .map { it.toUnapprovedProduct() }
            .singleOrNull()
            
        unapproved?.let { product ->
            // Remove from unapproved
            ProductsUnapproved.deleteWhere { ProductsUnapproved.id eq productId }
            
            // Add to approved
            val id = ProductsApproved.insertAndGetId {
                it[price] = product.price
                it[weight] = product.weight
                it[dimension] = product.dimension
                it[purity] = product.purity
                it[maxQuantity] = product.maxQuantity
                it[category] = product.category
            }.value
            
            // Return the approved product directly from the same transaction
            ProductsApproved.select { ProductsApproved.id eq id }
                .map { it.toApprovedProduct() }
                .single()
        }
    }
    
    // Read
    suspend fun getAllApprovedProducts(): List<ApprovedProduct> = transaction {
        ProductsApproved.selectAll().map { it.toApprovedProduct() }
    }
    
    suspend fun getAllUnapprovedProducts(): List<UnapprovedProduct> = transaction {
        ProductsUnapproved.selectAll().map { it.toUnapprovedProduct() }
    }
    
    suspend fun getApprovedProduct(id: UUID): ApprovedProduct? = transaction {
        ProductsApproved.select { ProductsApproved.id eq id }
            .map { it.toApprovedProduct() }
            .singleOrNull()
    }
    
    suspend fun getUnapprovedProduct(id: UUID): UnapprovedProduct? = transaction {
        ProductsUnapproved.select { ProductsUnapproved.id eq id }
            .map { it.toUnapprovedProduct() }
            .singleOrNull()
    }

    // Upsert helpers
    suspend fun upsertApproved(id: UUID, product: ApprovedProduct): ApprovedProduct = transaction {
        val existing = ProductsApproved.select { ProductsApproved.id eq id }.singleOrNull()
        if (existing == null) {
            ProductsApproved.insert {
                it[ProductsApproved.id] = id
                it[price] = product.price
                it[weight] = product.weight
                it[dimension] = product.dimension
                it[purity] = product.purity
                it[maxQuantity] = product.maxQuantity
                it[category] = product.category
                it[createdAt] = product.createdAt
                it[updatedAt] = product.updatedAt
            }
        } else {
            ProductsApproved.update({ ProductsApproved.id eq id }) {
                it[price] = product.price
                it[weight] = product.weight
                it[dimension] = product.dimension
                it[purity] = product.purity
                it[maxQuantity] = product.maxQuantity
                it[category] = product.category
                it[updatedAt] = java.time.LocalDateTime.now()
            }
        }
        // Return the latest row mapped to domain
        ProductsApproved.select { ProductsApproved.id eq id }
            .map { it.toApprovedProduct() }
            .single()
    }

    suspend fun upsertUnapproved(id: UUID, product: UnapprovedProduct): UnapprovedProduct = transaction {
        val existing = ProductsUnapproved.select { ProductsUnapproved.id eq id }.singleOrNull()
        if (existing == null) {
            ProductsUnapproved.insert {
                it[ProductsUnapproved.id] = id
                it[price] = product.price
                it[weight] = product.weight
                it[dimension] = product.dimension
                it[purity] = product.purity
                it[maxQuantity] = product.maxQuantity
                it[category] = product.category
                it[createdAt] = product.createdAt
                it[updatedAt] = product.updatedAt
            }
        } else {
            ProductsUnapproved.update({ ProductsUnapproved.id eq id }) {
                it[price] = product.price
                it[weight] = product.weight
                it[dimension] = product.dimension
                it[purity] = product.purity
                it[maxQuantity] = product.maxQuantity
                it[category] = product.category
                it[updatedAt] = java.time.LocalDateTime.now()
            }
        }
        // Return the latest row mapped to domain
        ProductsUnapproved.select { ProductsUnapproved.id eq id }
            .map { it.toUnapprovedProduct() }
            .single()
    }

    /**
     * Upsert both approved and unapproved with same id.
     * - If id is null, generate one and insert.
     * - If only one payload is provided on create, copy it to the other type.
     * - If updating and only one payload is provided, leave the other type unchanged.
     */
    suspend fun upsertBoth(
        id: UUID?,
        approved: ApprovedProduct?,
        unapproved: UnapprovedProduct?,
        isCreate: Boolean
    ): UUID {
        val productId = id ?: UUID.randomUUID()
        transaction {
            if (isCreate) {
                val now = java.time.LocalDateTime.now()
                val approvedToInsert = when {
                    approved != null -> approved.copy(id = productId, createdAt = approved.createdAt, updatedAt = approved.updatedAt)
                    unapproved != null -> ApprovedProduct(
                        id = productId,
                        price = unapproved.price,
                        weight = unapproved.weight,
                        dimension = unapproved.dimension,
                        purity = unapproved.purity,
                        maxQuantity = unapproved.maxQuantity,
                        category = unapproved.category,
                        createdAt = now,
                        updatedAt = now
                    )
                    else -> null
                }

                val unapprovedToInsert = when {
                    unapproved != null -> unapproved.copy(id = productId, createdAt = unapproved.createdAt, updatedAt = unapproved.updatedAt)
                    approved != null -> UnapprovedProduct(
                        id = productId,
                        price = approved.price,
                        weight = approved.weight,
                        dimension = approved.dimension,
                        purity = approved.purity,
                        maxQuantity = approved.maxQuantity,
                        category = approved.category,
                        createdAt = now,
                        updatedAt = now
                    )
                    else -> null
                }

                approvedToInsert?.let { ap ->
                    ProductsApproved.insert {
                        it[ProductsApproved.id] = productId
                        it[price] = ap.price
                        it[weight] = ap.weight
                        it[dimension] = ap.dimension
                        it[purity] = ap.purity
                        it[maxQuantity] = ap.maxQuantity
                        it[category] = ap.category
                        it[createdAt] = ap.createdAt
                        it[updatedAt] = ap.updatedAt
                    }
                }

                unapprovedToInsert?.let { up ->
                    ProductsUnapproved.insert {
                        it[ProductsUnapproved.id] = productId
                        it[price] = up.price
                        it[weight] = up.weight
                        it[dimension] = up.dimension
                        it[purity] = up.purity
                        it[maxQuantity] = up.maxQuantity
                        it[category] = up.category
                        it[createdAt] = up.createdAt
                        it[updatedAt] = up.updatedAt
                    }
                }
            } else {
                approved?.let { ap ->
                    val exists = ProductsApproved.select { ProductsApproved.id eq productId }.singleOrNull() != null
                    if (!exists) {
                        ProductsApproved.insert {
                            it[ProductsApproved.id] = productId
                            it[price] = ap.price
                            it[weight] = ap.weight
                            it[dimension] = ap.dimension
                            it[purity] = ap.purity
                            it[maxQuantity] = ap.maxQuantity
                            it[category] = ap.category
                            it[createdAt] = ap.createdAt
                            it[updatedAt] = java.time.LocalDateTime.now()
                        }
                    } else {
                        ProductsApproved.update({ ProductsApproved.id eq productId }) {
                            it[price] = ap.price
                            it[weight] = ap.weight
                            it[dimension] = ap.dimension
                            it[purity] = ap.purity
                            it[maxQuantity] = ap.maxQuantity
                            it[category] = ap.category
                            it[updatedAt] = java.time.LocalDateTime.now()
                        }
                    }
                }
                unapproved?.let { up ->
                    val exists = ProductsUnapproved.select { ProductsUnapproved.id eq productId }.singleOrNull() != null
                    if (!exists) {
                        ProductsUnapproved.insert {
                            it[ProductsUnapproved.id] = productId
                            it[price] = up.price
                            it[weight] = up.weight
                            it[dimension] = up.dimension
                            it[purity] = up.purity
                            it[maxQuantity] = up.maxQuantity
                            it[category] = up.category
                            it[createdAt] = up.createdAt
                            it[updatedAt] = java.time.LocalDateTime.now()
                        }
                    } else {
                        ProductsUnapproved.update({ ProductsUnapproved.id eq productId }) {
                            it[price] = up.price
                            it[weight] = up.weight
                            it[dimension] = up.dimension
                            it[purity] = up.purity
                            it[maxQuantity] = up.maxQuantity
                            it[category] = up.category
                            it[updatedAt] = java.time.LocalDateTime.now()
                        }
                    }
                }
            }
        }
        return productId
    }
    
    // Update
    suspend fun updateApprovedProduct(id: UUID, product: ApprovedProduct): Boolean = transaction {
        ProductsApproved.update({ ProductsApproved.id eq id }) {
            it[price] = product.price
            it[weight] = product.weight
            it[dimension] = product.dimension
            it[purity] = product.purity
            it[maxQuantity] = product.maxQuantity
            it[category] = product.category
            it[updatedAt] = java.time.LocalDateTime.now()
        } > 0
    }
    
    suspend fun updateUnapprovedProduct(id: UUID, product: UnapprovedProduct): Boolean = transaction {
        ProductsUnapproved.update({ ProductsUnapproved.id eq id }) {
            it[price] = product.price
            it[weight] = product.weight
            it[dimension] = product.dimension
            it[purity] = product.purity
            it[maxQuantity] = product.maxQuantity
            it[category] = product.category
            it[updatedAt] = java.time.LocalDateTime.now()
        } > 0
    }
    
    // Delete
    suspend fun deleteApprovedProduct(id: UUID): Boolean = transaction {
        ProductsApproved.deleteWhere { ProductsApproved.id eq id } > 0
    }
    
    suspend fun deleteUnapprovedProduct(id: UUID): Boolean = transaction {
        ProductsUnapproved.deleteWhere { ProductsUnapproved.id eq id } > 0
    }
    
    // Additional helper methods
    suspend fun moveToApproved(productId: UUID): Boolean {
        val unapproved = getUnapprovedProduct(productId) ?: return false
        
        transaction {
            // Delete from unapproved
            ProductsUnapproved.deleteWhere { ProductsUnapproved.id eq productId }
            
            // Add to approved
            ProductsApproved.insert {
                it[id] = unapproved.id
                it[price] = unapproved.price
                it[weight] = unapproved.weight
                it[dimension] = unapproved.dimension
                it[purity] = unapproved.purity
                it[maxQuantity] = unapproved.maxQuantity
                it[category] = unapproved.category
                it[createdAt] = unapproved.createdAt
                it[updatedAt] = java.time.LocalDateTime.now()
            }
        }
        
        return true
    }
}
