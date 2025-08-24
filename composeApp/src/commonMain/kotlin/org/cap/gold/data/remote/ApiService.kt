package org.cap.gold.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.cap.gold.data.model.Product
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.serialization.Serializable
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

@Serializable
data class SimpleProductResponse(
    val id: String? = null,
    val price: Double,
    val weight: Double,
    val dimension: String,
    val purity: String,
    val maxQuantity: Int,
    val category: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class BothVariantsResponse(
    val id: String,
    val approved: SimpleProductResponse? = null,
    val unapproved: SimpleProductResponse? = null
)

/**
 * Service interface for product-related API operations
 */
interface ProductApiService {
    suspend fun getApprovedProducts(): List<Product>
    suspend fun getUnapprovedProducts(): List<Product>
    suspend fun getProductById(id: String): Product
    suspend fun getUnapprovedProductById(id: String): Product
    suspend fun createApprovedProduct(product: Product): Product
    suspend fun createUnapprovedProduct(product: Product): Product
    suspend fun updateApprovedProduct(id: String, product: Product): Product
    suspend fun updateUnapprovedProduct(id: String, product: Product): Product
    suspend fun deleteApprovedProduct(id: String): Boolean
    suspend fun deleteUnapprovedProduct(id: String): Boolean
    // Admin endpoints to manage both variants together
    suspend fun getBothVariantsById(id: String): BothVariantsResponse
    suspend fun createBothVariants(id: String? = null, approved: Product?, unapproved: Product?): String
    suspend fun updateBothVariants(id: String, approved: Product?, unapproved: Product?): String
    // Order-related (stub for now)
    suspend fun createOrder(
        productId: String,
        quantity: Int,
        address: String,
        phoneNumber: String,
        name: String
    ): Order
}

/**
 * Implementation of [ProductApiService] using Ktor HTTP client
 */
class ProductApiServiceImpl(
    private val client: HttpClient
) : ProductApiService, KoinComponent {
    
    override suspend fun getApprovedProducts(): List<Product> =
        client.get("api/products/approved").body()

    override suspend fun getUnapprovedProducts(): List<Product> =
        client.get("api/products/unapproved").body()

    override suspend fun getProductById(id: String): Product =
        client.get("api/products/approved/$id").body()

    override suspend fun getUnapprovedProductById(id: String): Product =
        client.get("api/products/unapproved/$id").body()

    @Serializable
    private data class ProductPayload(
        val price: Double,
        val weight: Double,
        val dimension: String,
        val purity: String,
        val maxQuantity: Int,
        val category: String
    )

    

    @Serializable
    private data class UpsertBothRequest(
        val id: String? = null,
        val approved: ProductPayload? = null,
        val unapproved: ProductPayload? = null
    )

    private fun Product.toPayload(): ProductPayload = ProductPayload(
        price = this.price,
        weight = this.weight,
        dimension = this.dimension,
        purity = this.purity.toString(),
        maxQuantity = this.maxQuantity,
        category = this.category
    )

    override suspend fun createApprovedProduct(product: Product): Product =
        client.post("api/products/approved") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body()

    override suspend fun createUnapprovedProduct(product: Product): Product =
        client.post("api/products/unapproved") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body()

    override suspend fun updateApprovedProduct(id: String, product: Product): Product =
        client.put("api/products/approved/$id") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body()

    override suspend fun updateUnapprovedProduct(id: String, product: Product): Product =
        client.put("api/products/unapproved/$id") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body()

    override suspend fun deleteApprovedProduct(id: String): Boolean {
        val response: HttpResponse = client.delete("api/products/approved/$id")
        return response.status.isSuccess()
    }

    override suspend fun deleteUnapprovedProduct(id: String): Boolean {
        val response: HttpResponse = client.delete("api/products/unapproved/$id")
        return response.status.isSuccess()
    }

    // ===== Admin both-variant endpoints =====
    override suspend fun getBothVariantsById(id: String): BothVariantsResponse =
        client.get("api/products/admin/$id/both").body()

    override suspend fun createBothVariants(
        id: String?,
        approved: Product?,
        unapproved: Product?
    ): String {
        val payload = UpsertBothRequest(
            id = id,
            approved = approved?.toPayload(),
            unapproved = unapproved?.toPayload()
        )
        val resp: HttpResponse = client.post("api/products/admin/both") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        // Expect { id: String }
        @Serializable data class IdResp(val id: String)
        return try { resp.body<IdResp>().id } catch (_: Exception) {
            // fallback parse
            val text = resp.bodyAsText()
            // naive extraction
            Regex("\"id\"\\s*:\\s*\"([a-fA-F0-9-]+)\"").find(text)?.groupValues?.getOrNull(1)
                ?: ""
        }
    }

    override suspend fun updateBothVariants(
        id: String,
        approved: Product?,
        unapproved: Product?
    ): String {
        val payload = UpsertBothRequest(
            approved = approved?.toPayload(),
            unapproved = unapproved?.toPayload()
        )
        val resp: HttpResponse = client.put("api/products/admin/$id/both") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        @Serializable data class IdResp(val id: String)
        return try { resp.body<IdResp>().id } catch (_: Exception) {
            val text = resp.bodyAsText()
            Regex("\"id\"\\s*:\\s*\"([a-fA-F0-9-]+)\"").find(text)?.groupValues?.getOrNull(1)
                ?: id
        }
    }

    override suspend fun createOrder(
        productId: String,
        quantity: Int,
        address: String,
        phoneNumber: String,
        name: String
    ): Order {
        // Fetch product to assemble order payload (server expects product details)
        val product = runCatching { getProductById(productId) }
            .getOrElse { getUnapprovedProductById(productId) }

        @Serializable
        data class CreateOrderPayload(
            val productId: String,
            val productName: String,
            val productPrice: Double,
            val productWeight: Double,
            val productDimensions: String,
            val productQuantity: Int,
            val userMobile: String,
            val userName: String,
            val totalAmount: Double
        )

        val payload = CreateOrderPayload(
            productId = productId,
            productName = product.name,
            productPrice = product.price,
            productWeight = product.weight,
            productDimensions = product.dimension,
            productQuantity = quantity,
            userMobile = phoneNumber,
            userName = name,
            totalAmount = product.price * quantity
        )

        return client.post("api/orders") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }
}

