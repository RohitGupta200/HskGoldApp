package org.cap.gold.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.cap.gold.models.*
import org.cap.gold.repositories.ProductRepository
import java.util.*
import java.time.LocalDateTime

class ProductController(private val productRepository: ProductRepository) {
    
    fun Route.productRoutes() {
        // Request DTO aligned with client payload (incoming)
        @Serializable
        data class CreateProductRequest(
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String
        )

        // Generic payload used for admin upsert-both
        @Serializable
        data class ProductPayload(
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String
        )

        @Serializable
        data class UpsertBothRequest(
            val id: String? = null,
            val approved: ProductPayload? = null,
            val unapproved: ProductPayload? = null
        )

        // Update DTO to avoid receiving domain models with LocalDateTime
        @Serializable
        data class UpdateProductRequest(
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String
        )

        // Response DTOs (serializable) to avoid issues with UUID/LocalDateTime
        @Serializable
        data class ApprovedProductResponse(
            val id: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val createdAt: String,
            val updatedAt: String
        )

        @Serializable
        data class UnapprovedProductResponse(
            val id: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val createdAt: String,
            val updatedAt: String
        )

        fun ApprovedProduct.toResponse() = ApprovedProductResponse(
            id = id.toString(),
            price = price,
            weight = weight,
            dimension = dimension,
            purity = purity,
            maxQuantity = maxQuantity,
            category = category,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )

        fun UnapprovedProduct.toResponse() = UnapprovedProductResponse(
            id = id.toString(),
            price = price,
            weight = weight,
            dimension = dimension,
            purity = purity,
            maxQuantity = maxQuantity,
            category = category,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )

        route("/products") {
            // ===== Admin endpoints to manage both variants with same ID =====
            route("/admin") {
                // Get both variants by id
                get("{id}/both") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        ?: throw IllegalArgumentException("Invalid ID format")
                    val approved = productRepository.getApprovedProduct(id)
                    val unapproved = productRepository.getUnapprovedProduct(id)
                    if (approved == null && unapproved == null) {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                        return@get
                    }
                    @Serializable
                    data class BothResponse(
                        val id: String,
                        val approved: ApprovedProductResponse? = null,
                        val unapproved: UnapprovedProductResponse? = null
                    )
                    call.respond(
                        BothResponse(
                            id = id.toString(),
                            approved = approved?.toResponse(),
                            unapproved = unapproved?.toResponse()
                        )
                    )
                }

                // Create both (or copy one to the other if one missing)
                post("both") {
                    val req = call.receive<UpsertBothRequest>()
                    val now = LocalDateTime.now()
                    val approvedModel = req.approved?.let {
                        ApprovedProduct(
                            id = UUID.randomUUID(),
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    val unapprovedModel = req.unapproved?.let {
                        UnapprovedProduct(
                            id = UUID.randomUUID(),
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    val id = productRepository.upsertBoth(
                        id = req.id?.let { UUID.fromString(it) },
                        approved = approvedModel,
                        unapproved = unapprovedModel,
                        isCreate = true
                    )
                    call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
                }

                // Update both by id (only provided parts change)
                put("{id}/both") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        ?: throw IllegalArgumentException("Invalid ID format")
                    val req = call.receive<UpsertBothRequest>()
                    val now = LocalDateTime.now()
                    val approvedModel = req.approved?.let {
                        // Keep createdAt from DB where possible
                        val existing = productRepository.getApprovedProduct(id)
                        ApprovedProduct(
                            id = id,
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    }
                    val unapprovedModel = req.unapproved?.let {
                        val existing = productRepository.getUnapprovedProduct(id)
                        UnapprovedProduct(
                            id = id,
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    }
                    productRepository.upsertBoth(
                        id = id,
                        approved = approvedModel,
                        unapproved = unapprovedModel,
                        isCreate = false
                    )
                    call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
                }
            }

            // Approved products endpoints
            route("/approved") {
                // Get all approved products
                get {
                    val products = productRepository.getAllApprovedProducts()
                    call.respond(products.map { it.toResponse() })
                }
                
                // Create a new approved product
                post {
                    val req = call.receive<CreateProductRequest>()
                    val now = LocalDateTime.now()
                    val newProduct = ApprovedProduct(
                        id = UUID.randomUUID(),
                        price = req.price,
                        weight = req.weight,
                        dimension = req.dimension,
                        purity = req.purity,
                        maxQuantity = req.maxQuantity,
                        category = req.category,
                        createdAt = now,
                        updatedAt = now
                    )
                    val createdProduct = productRepository.createApprovedProduct(newProduct)
                    call.respond(HttpStatusCode.Created, createdProduct.toResponse())
                }
                
                // Get a single approved product
                get("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    
                    val product = productRepository.getApprovedProduct(id)
                    if (product != null) {
                        call.respond(product.toResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
                
                // Update an approved product
                put("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    // fetch existing to preserve createdAt
                    val existing = productRepository.getApprovedProduct(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, "Product not found")

                    val dto = call.receive<UpdateProductRequest>()
                    val product = ApprovedProduct(
                        id = id,
                        price = dto.price,
                        weight = dto.weight,
                        dimension = dto.dimension,
                        purity = dto.purity,
                        maxQuantity = dto.maxQuantity,
                        category = dto.category,
                        createdAt = existing.createdAt,
                        updatedAt = LocalDateTime.now()
                    )
                    if (productRepository.updateApprovedProduct(id, product)) {
                        call.respond(HttpStatusCode.OK, "Product updated successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
                
                // Delete an approved product
                delete("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    
                    if (productRepository.deleteApprovedProduct(id)) {
                        call.respond(HttpStatusCode.OK, "Product deleted successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
            }
            
            // Unapproved products endpoints
            route("/unapproved") {
                // Get all unapproved products
                get {
                    val products = productRepository.getAllUnapprovedProducts()
                    call.respond(products.map { it.toResponse() })
                }
                
                // Get a single unapproved product
                get("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    
                    val product = productRepository.getUnapprovedProduct(id)
                    if (product != null) {
                        call.respond(product.toResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
                
                // Create a new unapproved product
                post {
                    val req = call.receive<CreateProductRequest>()
                    val now = LocalDateTime.now()
                    val newProduct = UnapprovedProduct(
                        id = UUID.randomUUID(),
                        price = req.price,
                        weight = req.weight,
                        dimension = req.dimension,
                        purity = req.purity,
                        maxQuantity = req.maxQuantity,
                        category = req.category,
                        createdAt = now,
                        updatedAt = now
                    )
                    val createdProduct = productRepository.createUnapprovedProduct(newProduct)
                    call.respond(HttpStatusCode.Created, createdProduct.toResponse())
                }
                
                // Update an unapproved product
                put("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    // fetch existing to preserve createdAt
                    val existing = productRepository.getUnapprovedProduct(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, "Product not found")

                    val dto = call.receive<UpdateProductRequest>()
                    val product = UnapprovedProduct(
                        id = id,
                        price = dto.price,
                        weight = dto.weight,
                        dimension = dto.dimension,
                        purity = dto.purity,
                        maxQuantity = dto.maxQuantity,
                        category = dto.category,
                        createdAt = existing.createdAt,
                        updatedAt = LocalDateTime.now()
                    )
                    if (productRepository.updateUnapprovedProduct(id, product)) {
                        call.respond(HttpStatusCode.OK, "Product updated successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
                
                // Delete an unapproved product
                delete("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    
                    if (productRepository.deleteUnapprovedProduct(id)) {
                        call.respond(HttpStatusCode.OK, "Product deleted successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
                
                // Approve a product (move from unapproved to approved)
                post("{id}/approve") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    
                    val approvedProduct = productRepository.approveProduct(id)
                    if (approvedProduct != null) {
                        call.respond(approvedProduct.toResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Product not found")
                    }
                }
            }
        }
    }
}
