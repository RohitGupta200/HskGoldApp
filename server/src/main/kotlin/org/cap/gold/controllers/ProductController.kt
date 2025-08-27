package org.cap.gold.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.cap.gold.models.*
import org.cap.gold.repositories.ProductRepository
import org.cap.gold.service.NotificationService
import java.util.*
import java.time.LocalDateTime
import java.util.Base64

class ProductController(
    private val productRepository: ProductRepository,
    private val notificationService: NotificationService
) {
    
    fun Route.productRoutes() {
        // Request DTO aligned with client payload (incoming)
        @Serializable
        data class CreateProductRequest(
            val name: String,
            val description: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val customFields: String
        )

        // Generic payload used for admin upsert-both
        @Serializable
        data class ProductPayload(
            val name: String,
            val description: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val customFields: String
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
            val name: String,
            val description: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val customFields: String
        )

        // Response DTOs (serializable) to avoid issues with UUID/LocalDateTime
        @Serializable
        data class ApprovedProductResponse(
            val id: String,
            val name: String,
            val description: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val customFields: String,
            val createdAt: String,
            val updatedAt: String,
            val imageBase64: String? = null
        )

        @Serializable
        data class UnapprovedProductResponse(
            val id: String,
            val name: String,
            val description: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val customFields: String,
            val createdAt: String,
            val updatedAt: String,
            val imageBase64: String? = null
        )

        @Serializable
        data class ProductResponse(
            val id: String,
            val name: String,
            val description: String,
            val price: Double,
            val weight: Double,
            val dimension: String,
            val purity: String,
            val maxQuantity: Int,
            val category: String,
            val customFields: String,
            val createdAt: String,
            val updatedAt: String,
            val imageBase64: String? = null
        )

        @Serializable
        data class ProductListResponse(
            val id: String,
            val name: String,

            val price: Double,

            val category: String,
        )

        fun ApprovedProduct.toResponse(imageBytes: ByteArray? = null) = ApprovedProductResponse(
            id = id.toString(),
            name = name,
            description = description,
            price = price,
            weight = weight,
            dimension = dimension,
            purity = purity,
            maxQuantity = maxQuantity,
            category = category,
            customFields = customFields,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            imageBase64 = imageBytes?.let { Base64.getEncoder().encodeToString(it) }
        )

        fun UnapprovedProduct.toResponse(imageBytes: ByteArray? = null) = UnapprovedProductResponse(
            id = id.toString(),
            name = name,
            description = description,
            price = price,
            weight = weight,
            dimension = dimension,
            purity = purity,
            maxQuantity = maxQuantity,
            category = category,
            customFields = customFields,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            imageBase64 = imageBytes?.let { Base64.getEncoder().encodeToString(it) }
        )

        fun ProductWithOutImageForList.toResponse() = ProductListResponse(
            id = id.toString(),
            name = name,
            price = price,
            category = category,
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
                    val img = productRepository.getImage(id)
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
                            approved = approved?.toResponse(img),
                            unapproved = unapproved?.toResponse(img)
                        )
                    )
                }

                // Create both (or copy one to the other if one missing)
                post("both") {
                    // Support multipart (json + image) and pure JSON for backward compatibility
                    val contentType = call.request.contentType()
                    var req: UpsertBothRequest? = null
                    var imageBytes: ByteArray? = null

                    if (contentType.match(ContentType.MultiPart.FormData)) {
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FileItem -> {
                                    imageBytes = part.streamProvider().readBytes()
                                }
                                is PartData.FormItem -> {
                                    if (part.name == "json") {
                                        req = Json { ignoreUnknownKeys = true }.decodeFromString(UpsertBothRequest.serializer(), part.value)
                                    }
                                }
                                else -> {}
                            }
                            part.dispose()
                        }
                        if (req == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing json payload part")
                            return@post
                        }
                    } else {
                        req = call.receive<UpsertBothRequest>()
                    }
                    val now = LocalDateTime.now()
                    val approvedModel = req!!.approved?.let {
                        ApprovedProduct(
                            id = UUID.randomUUID(),
                            name = it.name,
                            description = it.description,
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            customFields = it.customFields,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    val unapprovedModel = req!!.unapproved?.let {
                        UnapprovedProduct(
                            id = UUID.randomUUID(),
                            name = it.name,
                            description = it.description,
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            customFields = it.customFields,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    val id = productRepository.upsertBoth(
                        id = req!!.id?.let { UUID.fromString(it) },
                        approved = approvedModel,
                        unapproved = unapprovedModel,
                        isCreate = true
                    )
                    imageBytes?.let { productRepository.upsertImage(id, it) }
                    // Fire and forget: notify admins that products have changed

                    call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
                }

                // Update both by id (only provided parts change)
                put("{id}/both") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        ?: throw IllegalArgumentException("Invalid ID format")
                    val contentType = call.request.contentType()
                    var req: UpsertBothRequest? = null
                    var imageBytes: ByteArray? = null
                    if (contentType.match(ContentType.MultiPart.FormData)) {
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FileItem -> {
                                    imageBytes = part.streamProvider().readBytes()
                                }
                                is PartData.FormItem -> {
                                    if (part.name == "json") {
                                        req = Json { ignoreUnknownKeys = true }.decodeFromString(UpsertBothRequest.serializer(), part.value)
                                    }
                                }
                                else -> {}
                            }
                            part.dispose()
                        }
                        if (req == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing json payload part")
                            return@put
                        }
                    } else {
                        req = call.receive<UpsertBothRequest>()
                    }
                    val now = LocalDateTime.now()
                    val approvedModel = req!!.approved?.let {
                        // Keep createdAt from DB where possible
                        val existing = productRepository.getApprovedProduct(id)
                        ApprovedProduct(
                            id = id,
                            name = it.name,
                            description = it.description,
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            customFields = it.customFields,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    }
                    val unapprovedModel = req!!.unapproved?.let {
                        val existing = productRepository.getUnapprovedProduct(id)
                        UnapprovedProduct(
                            id = id,
                            name = it.name,
                            description = it.description,
                            price = it.price,
                            weight = it.weight,
                            dimension = it.dimension,
                            purity = it.purity,
                            maxQuantity = it.maxQuantity,
                            category = it.category,
                            customFields = it.customFields,
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
                    imageBytes?.let { productRepository.upsertImage(id, it) }
                    // Fire and forget: notify admins that products have changed

                    call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
                }
            }

            // Approved products endpoints
            route("/approved") {
                // Get all approved products
                get {
                    /*val products = productRepository.getAllApprovedProducts()
                    val response = products.map { p ->
                        val img = productRepository.getImage(p.id)
                        p.toResponse(img)
                    }*/
                    val products = productRepository.getApprovedProductsWithImage()
                    val response = products.map { it.toResponse() }
                    call.respond(response)
                }
                
                // Create a new approved product
                post {
                    val req = call.receive<CreateProductRequest>()
                    val now = LocalDateTime.now()
                    val newProduct = ApprovedProduct(
                        id = UUID.randomUUID(),
                        name = req.name,
                        description = req.description,
                        price = req.price,
                        weight = req.weight,
                        dimension = req.dimension,
                        purity = req.purity,
                        maxQuantity = req.maxQuantity,
                        category = req.category,
                        customFields = req.customFields,
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
                        val img = productRepository.getImage(product.id)
                        call.respond(product.toResponse(img))
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
                        name = dto.name,
                        description = dto.description,
                        price = dto.price,
                        weight = dto.weight,
                        dimension = dto.dimension,
                        purity = dto.purity,
                        maxQuantity = dto.maxQuantity,
                        category = dto.category,
                        customFields = dto.customFields,
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
                    val products = productRepository.getUnapprovedProductsWithImage()
                    val response = products.map { it.toResponse() }
                    call.respond(response)
                }
                
                // Get a single unapproved product
                get("{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) } 
                        ?: throw IllegalArgumentException("Invalid ID format")
                    val product = productRepository.getUnapprovedProduct(id)
                    if (product != null) {
                        val img = productRepository.getImage(product.id)
                        call.respond(product.toResponse(img))
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
                        name = req.name,
                        description = req.description,
                        price = req.price,
                        weight = req.weight,
                        dimension = req.dimension,
                        purity = req.purity,
                        maxQuantity = req.maxQuantity,
                        category = req.category,
                        customFields = req.customFields,
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
                        name = dto.name,
                        description = dto.description,
                        price = dto.price,
                        weight = dto.weight,
                        dimension = dto.dimension,
                        purity = dto.purity,
                        maxQuantity = dto.maxQuantity,
                        category = dto.category,
                        customFields = dto.customFields,
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

            // Public: fetch product image bytes by product ID
            get("{id}/image") {
                val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Invalid ID format")
                val img = productRepository.getImage(id)
                if (img == null) {
                    call.respond(HttpStatusCode.NotFound, "Image not found")
                } else {
                    call.respondBytes(bytes = img, contentType = ContentType.Image.Any)
                }
            }
        }
    }
}
