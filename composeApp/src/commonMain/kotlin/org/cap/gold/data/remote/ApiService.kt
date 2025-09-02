package org.cap.gold.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.client.plugins.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.cap.gold.cache.ClientCache
import org.cap.gold.data.model.ListProduct
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.model.Product
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class SimpleProductResponse(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val price: Double,
    val margin: Double = 0.0,
    val multiplier: Double = 1.0,
    val weight: String,
    val dimension: String,
    val purity: String,
    val maxQuantity: Int,
    val category: String,
    val imageBase64: String? = null,
    val customFields: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class ListProductResponse(
    val id: String? = null,
    val name: String = "",
    val price: Double? = 0.0,
    val margin: Double? = 0.0,
    val multiplier: Double? = 1.0,
    val category: String? = "",
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
    suspend fun getProductImage(id: String): ByteArray?
    suspend fun createApprovedProduct(product: Product): Product
    suspend fun createUnapprovedProduct(product: Product): Product
    suspend fun updateApprovedProduct(id: String, product: Product): Product
    suspend fun updateUnapprovedProduct(id: String, product: Product): Product
    suspend fun deleteApprovedProduct(id: String): Boolean
    suspend fun deleteUnapprovedProduct(id: String): Boolean
    // Admin endpoints to manage both variants together
    suspend fun getBothVariantsById(id: String): BothVariantsResponse
    suspend fun createBothVariants(
        id: String? = null,
        approved: Product?,
        unapproved: Product?,
        imageBytes: ByteArray? = null,
        imageFileName: String? = null,
        imageContentType: ContentType = ContentType.Image.Any,
        approvedCustomFields: String? = null,
        unapprovedCustomFields: String? = null,
        applyToAll: Boolean = false
    ): String
    suspend fun updateBothVariants(
        id: String,
        approved: Product?,
        unapproved: Product?,
        imageBytes: ByteArray? = null,
        imageFileName: String? = null,
        imageContentType: ContentType = ContentType.Image.Any,
        approvedCustomFields: String? = null,
        unapprovedCustomFields: String? = null,
        applyToAll: Boolean = false
    ): String
    // Order-related (stub for now)
    suspend fun createOrder(
        productId: String,
        quantity: Int,
        address: String,
        phoneNumber: String,
        name: String,
        isApprovedUser: Boolean
    ): Order

    // About Us
    suspend fun getAboutUs(): String
    suspend fun setAboutUs(content: String): Boolean
}

/**
 * Implementation of [ProductApiService] using Ktor HTTP client
 */
class ProductApiServiceImpl(
    private val client: HttpClient
) : ProductApiService, KoinComponent {
    
    override suspend fun getApprovedProducts(): List<Product> {
        val key = "GET:/api/products/approved"
        val ttlSeconds = 10L // 5 minutes
        // Try cached JSON first
        val cached = ClientCache.getFresh(key)
        if (cached != null) {
            return try {
                Json { ignoreUnknownKeys = true }
                    .decodeFromString(ListSerializer(ListProductResponse.serializer()), cached)
                    .map { it.toProduct(it.id ?: "") }
            } catch (_: Exception) {
                // fall through to network
                emptyList()
            }.ifEmpty {
                // If parsing failed to produce data, fetch from network
                val text = client.get("api/products/approved") {
                    header("X-Cache-Ttl", ttlSeconds.toString())
                }.bodyAsText()
                // Store in cache
                ClientCache.put(key, text, ttlSeconds)
                Json { ignoreUnknownKeys = true }
                    .decodeFromString(ListSerializer(ListProductResponse.serializer()), text)
                    .map { it.toProduct(it.id ?: "") }
            }
        }
        // No cache hit; fetch and cache
        val text = client.get("api/products/approved") {
            header("X-Cache-Ttl", ttlSeconds.toString())
        }.bodyAsText()
        ClientCache.put(key, text, ttlSeconds)
        return Json { ignoreUnknownKeys = true }
            .decodeFromString(ListSerializer(ListProductResponse.serializer()), text)
            .map { it.toProduct(it.id ?: "") }
    }

    override suspend fun getUnapprovedProducts(): List<Product> =
        client.get("api/products/unapproved").body<List<ListProductResponse>>().map { it.toProduct(it.id ?: "") }

    override suspend fun getProductById(id: String): Product =
        client.get("api/products/approved/$id").body<SimpleProductResponse>().toProduct(id)

    override suspend fun getUnapprovedProductById(id: String): Product =
        client.get("api/products/unapproved/$id").body<SimpleProductResponse>().toProduct(id)

    override suspend fun getProductImage(id: String): ByteArray? {
        val key = "GET:/api/products/$id/image"
        val ttlSeconds = 3600L // 60 minutes
        // Cache hit
        val cached = ClientCache.getFresh(key)
        if (cached != null) {
            return try { cached.decodeBase64Bytes() } catch (_: Throwable) { null }
        }
        // Fetch from server and handle non-2xx via exceptions
        return try {
            val resp: HttpResponse = client.get("api/products/$id/image")
            val bytes: ByteArray = resp.body()
            ClientCache.put(key, bytes.encodeBase64(), ttlSeconds)
            bytes
        } catch (e: ClientRequestException) { // 4xx
            if (e.response.status == HttpStatusCode.NotFound) null else null
        } catch (e: ServerResponseException) { // 5xx
            null
        }
    }

    @Serializable
    private data class ProductPayload(
        val name: String,
        val description: String,
        val price: Double,
        val margin: Double = 0.0,
        val multiplier: Double = 1.0,
        val weight: String,
        val dimension: String,
        val purity: String,
        val maxQuantity: Int,
        val category: String,
        val customFields: String = "",
    )

    private fun SimpleProductResponse.toProduct(id: String): Product = Product(
        id = id,
        name = this.name,
        price = this.price,
        margin = this.margin,
        multiplier = this.multiplier,
        imageUrl = "",
        imageBase64 = this.imageBase64,
        category = this.category,
        description = this.description,
        weight = this.weight,
        purity = this.purity,
        dimension = this.dimension,
        maxQuantity = this.maxQuantity
    )

    private fun ListProductResponse.toProduct(id: String): Product = Product(
        id = id,
        name = this.name,
        price = this.price?:0.0,
        margin = this.margin?:0.0,
        multiplier = this.multiplier?:1.0,
        category = this.category?:"Uncategorized",
    )

    

    @Serializable
    private data class UpsertBothRequest(
        val id: String? = null,
        val approved: ProductPayload? = null,
        val unapproved: ProductPayload? = null,
        val applyToAll: Boolean? = false
    )

    private fun Product.toPayload(customFields: String? = null): ProductPayload = ProductPayload(
        name = this.name,
        description = this.description,
        price = this.price,
        margin = this.margin,
        multiplier = this.multiplier,
        weight = this.weight,
        dimension = this.dimension,
        purity = this.purity,
        maxQuantity = this.maxQuantity,
        category = this.category,
        customFields = customFields ?: ""
    )

    override suspend fun createApprovedProduct(product: Product): Product =
        client.post("api/products/approved") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body<SimpleProductResponse>().let {
            // Invalidate approved list cache
            ClientCache.invalidate("GET:/api/products/approved")
            it.toProduct(it.id ?: "")
        }

    override suspend fun createUnapprovedProduct(product: Product): Product =
        client.post("api/products/unapproved") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body<SimpleProductResponse>().let { it.toProduct(it.id ?: "") }

    override suspend fun updateApprovedProduct(id: String, product: Product): Product =
        client.put("api/products/approved/$id") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body<SimpleProductResponse>().toProduct(id).also {
            ClientCache.invalidate("GET:/api/products/approved")
        }

    override suspend fun updateUnapprovedProduct(id: String, product: Product): Product =
        client.put("api/products/unapproved/$id") {
            contentType(ContentType.Application.Json)
            setBody(product.toPayload())
        }.body<SimpleProductResponse>().toProduct(id)

    override suspend fun deleteApprovedProduct(id: String): Boolean {
        val response: HttpResponse = client.delete("api/products/approved/$id")
        val ok = response.status.isSuccess()
        if (ok) ClientCache.invalidate("GET:/api/products/approved")
        return ok
    }

    override suspend fun deleteUnapprovedProduct(id: String): Boolean {
        val response: HttpResponse = client.delete("api/products/unapproved/$id")
        return response.status.isSuccess()
    }

    @Serializable
    private data class AboutUsPayload(val content: String)

    override suspend fun getAboutUs(): String = try {
        client.get("api/aboutus").body<AboutUsPayload>().content
    } catch (_: Exception) {
        // Fallback: try plain text
        try { client.get("api/aboutus").bodyAsText() } catch (_: Exception) { "" }
    }

    override suspend fun setAboutUs(content: String): Boolean = try {
        val resp: HttpResponse = client.post("api/aboutus") {
            contentType(ContentType.Application.Json)
            setBody(AboutUsPayload(content))
        }
        resp.status.isSuccess()
    } catch (_: Exception) { false }

    // ===== Admin both-variant endpoints =====
    override suspend fun getBothVariantsById(id: String): BothVariantsResponse =
        client.get("api/products/admin/$id/both").body()

    override suspend fun createBothVariants(
        id: String?,
        approved: Product?,
        unapproved: Product?,
        imageBytes: ByteArray?,
        imageFileName: String?,
        imageContentType: ContentType,
        approvedCustomFields: String?,
        unapprovedCustomFields: String?,
        applyToAll: Boolean
    ): String {
        val payload = UpsertBothRequest(
            id = id,
            approved = approved?.toPayload(approvedCustomFields),
            unapproved = unapproved?.toPayload(unapprovedCustomFields),
            applyToAll = applyToAll
        )
        val resp: HttpResponse = if (imageBytes != null && imageFileName != null) {
            val json = Json.encodeToString(UpsertBothRequest.serializer(), payload)
            client.post("api/products/admin/both") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "json",
                                json,
                                headersOf(
                                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
                                )
                            )
                            append(
                                "image",
                                imageBytes,
                                headersOf(
                                    HttpHeaders.ContentType to listOf(imageContentType.toString()),
                                    HttpHeaders.ContentDisposition to listOf("filename=\"$imageFileName\"")
                                )
                            )
                        }
                    )
                )
            }
        } else {
            client.post("api/products/admin/both") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
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
        unapproved: Product?,
        imageBytes: ByteArray?,
        imageFileName: String?,
        imageContentType: ContentType,
        approvedCustomFields: String?,
        unapprovedCustomFields: String?,
        applyToAll: Boolean
    ): String {
        val payload = UpsertBothRequest(
            approved = approved?.toPayload(approvedCustomFields),
            unapproved = unapproved?.toPayload(unapprovedCustomFields),
            applyToAll = applyToAll
        )
        val resp: HttpResponse = if (imageBytes != null && imageFileName != null) {
            val json = Json.encodeToString(UpsertBothRequest.serializer(), payload)
            client.put("api/products/admin/$id/both") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "json",
                                json,
                                headersOf(
                                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
                                )
                            )
                            append(
                                "image",
                                imageBytes,
                                headersOf(
                                    HttpHeaders.ContentType to listOf(imageContentType.toString()),
                                    HttpHeaders.ContentDisposition to listOf("filename=\"$imageFileName\"")
                                )
                            )
                        }
                    )
                )
            }
        } else {
            client.put("api/products/admin/$id/both") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
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
        name: String,
        isApprovedUser: Boolean
    ): Order {
        // Fetch product to assemble order payload (server expects product details)
        val product = if(isApprovedUser) runCatching { getProductById(productId) }.getOrElse { null }
            else runCatching { getUnapprovedProductById(productId) }.getOrElse { null }
        if(product == null){
            throw NullPointerException("Please Try Again")
            return Order()
        }

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
            productPrice = product.margin+((product.price)*product.multiplier),
            productWeight = product.weight.toDoubleOrNull() ?: 0.0,
            productDimensions = product.dimension,
            productQuantity = quantity,
            userMobile = phoneNumber,
            userName = name,
            totalAmount = (product.margin+((product.price)*product.multiplier)) * quantity
        )

        return client.post("api/orders") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }
}

