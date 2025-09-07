package org.cap.gold.ui.screens.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.model.Product
import org.cap.gold.data.remote.ProductApiService
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class ProductDetailViewModel(
    private val productApiService: ProductApiService,
    private val productId: String,
    val isAdmin: Boolean,
    val isApprovedUser: Boolean
) {

    data class Field(
        val label: String,
        val value: String
    )
    @Serializable
    private data class FieldDto(
        val label: String,
        val value: String
    )
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var product by mutableStateOf<Product?>(null)
    var unapprovedProduct by mutableStateOf<Product?>(null)
    var orderSuccess by mutableStateOf(false)
    var showOrderDialog by mutableStateOf(false)
    var quantity by mutableStateOf(1)
    // Admin option: when checked, sync unapproved.price to approved.price on save
    var syncPrices by mutableStateOf(false)

    val fields = mutableStateListOf<Field>()

    val fieldsUnapproved = mutableStateListOf<Field>()
    // Image selection state (admin edit/create)
    var selectedImageBytes by mutableStateOf<ByteArray?>(null)
        private set
    var selectedImageFileName by mutableStateOf<String?>(null)
        private set
    val isCreateMode: Boolean = productId == "new"
    
    init {
        if (isCreateMode && isAdmin) {
            // Initialize a blank editable product for creation
            product = Product(
                id = "",
                name = "",
                price = 0.0,
                imageUrl = "",
                category = "",
                description = "",
                weight = "",
                purity = "",
                dimension = "",
                maxQuantity = 1
            )
        } else {
            loadProduct()
        }
    }
    fun onImageSelected(bytes: ByteArray, fileName: String) {
        selectedImageBytes = bytes
        selectedImageFileName = fileName
        // also preview in current product as data URI base64
        product = product?.copy(
            imageBase64 = bytes.encodeBase64()
        )
    }
    
    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.encodeBase64(): String = kotlin.io.encoding.Base64.encode(this)
    
    fun loadProduct() {
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                product = if (isAdmin) {
                    // Admin: fetch both and prefer approved if present
                    val both = try { productApiService.getBothVariantsById(productId) } catch (_: Exception) { null }
                    val p = when {
                        both == null -> null
                        both.approved != null -> Product(
                            id = productId,
                            name = both.approved.name,
                            price = both.approved.price,
                            imageUrl = "",
                            imageBase64 = both.approved.imageBase64,
                            category = both.approved.category,
                            description = both.approved.description,
                            weight = both.approved.weight,
                            purity = both.approved.purity,
                            dimension = both.approved.dimension,
                            maxQuantity = both.approved.maxQuantity,
                            multiplier = both.approved.multiplier,
                            margin = both.approved.margin
                        )
                        both.unapproved != null -> Product(
                            id = productId,
                            name = both.unapproved.name,
                            price = both.unapproved.price,
                            imageUrl = "",
                            imageBase64 = both.unapproved.imageBase64,
                            category = both.unapproved.category,
                            description = both.unapproved.description,
                            weight = both.unapproved.weight,
                            purity = both.unapproved.purity,
                            dimension = both.unapproved.dimension,
                            maxQuantity = both.unapproved.maxQuantity,
                            multiplier = both.unapproved.multiplier,
                            margin = both.unapproved.margin
                        )
                        else -> null
                    }
                    unapprovedProduct = if(both?.unapproved != null) Product(
                        id = productId,
                        name = both.unapproved.name,
                        price = both.unapproved.price,
                        imageUrl = "",
                        imageBase64 = both.unapproved.imageBase64,
                        category = both.unapproved.category,
                        description = both.unapproved.description,
                        weight = both.unapproved.weight,
                        purity = both.unapproved.purity,
                        dimension = both.unapproved.dimension,
                        maxQuantity = both.unapproved.maxQuantity,
                        multiplier = both.unapproved.multiplier,
                        margin = both.unapproved.margin
                    )
                    else null
                    // Populate fields from customFields JSON
                    val fieldsJson = both?.approved?.customFields
                    setFieldsFromJson(fieldsJson)
                    setUnApprovedFieldsFromJson(both?.unapproved?.customFields)
                    p
                } else if (isApprovedUser) {
                    val p = productApiService.getProductById(productId)
                    // Fetch customFields from approved variant
                    val both = runCatching { productApiService.getBothVariantsById(productId) }.getOrNull()
                    val fieldsJson = both?.approved?.customFields
                    setFieldsFromJson(fieldsJson)
                    p
                } else {
                    val p = productApiService.getUnapprovedProductById(productId)
                    // Fetch customFields from unapproved variant
                    val both = runCatching { productApiService.getBothVariantsById(productId) }.getOrNull()
                    val fieldsJson = both?.unapproved?.customFields
                    setFieldsFromJson(fieldsJson)
                    p
                }
                // Debug: log image info received from API
                runCatching {
                    val len = product?.imageBase64?.length ?: 0
                    println("DEBUG ProductDetail.loadProduct: id=$productId base64Len=$len imageUrl='${product?.imageUrl}' isAdmin=$isAdmin isApprovedUser=$isApprovedUser")
                }
                
                if (product == null) {
                    error = "Product not found"
                }
            } catch (e: Exception) {
                error = "Failed to load product: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun updateProduct(updatedProduct: Product, onSuccess: () -> Unit) {
        if (!isAdmin) return
        
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val approvedFieldsJson = buildCustomFieldsJson()
                if (isCreateMode) {
                    // Create via both-variants API; send only one side to allow copy
                    val newId = productApiService.createBothVariants(
                        id = null,
                        approved = updatedProduct,
                        unapproved = null,
                        imageBytes = selectedImageBytes,
                        imageFileName = selectedImageFileName,
                        approvedCustomFields = approvedFieldsJson,
                        unapprovedCustomFields = null
                    )
                    // Load created product (approved preferred)
                    product = productApiService.getProductById(newId)
                } else {
                    // Update via both-variants API; here we send approved variant only by default
                    productApiService.updateBothVariants(
                        id = productId,
                        approved = updatedProduct,
                        unapproved = null,
                        imageBytes = selectedImageBytes,
                        imageFileName = selectedImageFileName,
                        approvedCustomFields = approvedFieldsJson,
                        unapprovedCustomFields = null
                    )
                    // Refresh current product
                    product = productApiService.getProductById(productId)
                }
                // Debug: log image info after create/update
                runCatching {
                    val len = product?.imageBase64?.length ?: 0
                    println("DEBUG ProductDetail.updateProduct: id=${product?.id} base64Len=$len imageUrl='${product?.imageUrl}'")
                }
                onSuccess()
                // Clear selection after successful save
                selectedImageBytes = null
                selectedImageFileName = null
            } catch (e: Exception) {
                error = "Failed to update product: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun deleteProduct(onSuccess: () -> Unit) {
        if (!isAdmin) return

        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                // Attempt to delete both approved and unapproved variants
                val approvedDeleted = runCatching { productApiService.deleteApprovedProduct(productId) }.isSuccess
                val unapprovedDeleted = runCatching { productApiService.deleteUnapprovedProduct(productId) }.isSuccess

                if (approvedDeleted || unapprovedDeleted) {
                    onSuccess()
                } else {
                    error = "Failed to delete product. It may have already been removed."
                }
            } catch (e: Exception) {
                error = "An unexpected error occurred during deletion: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


    
    fun placeOrder(userPhone: String, userName: String, onSuccess: (Order) -> Unit) {        
        val currentProduct = product ?: return
        
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                // First verify the price
                val latestProduct = if (isApprovedUser) {
                    productApiService.getProductById(productId)
                } else {
                    productApiService.getUnapprovedProductById(productId)
                }
                
                if (latestProduct.price != currentProduct.price) {
                    // Update local product with latest data
                    product = latestProduct
                    error = "Product price has been updated. Please review the new price and try again."
                    return@launch
                }
                
                // Create order
                val order = productApiService.createOrder(
                    productId = productId,
                    quantity = quantity,
                    address = "", // Address is no longer collected from the user
                    phoneNumber = userPhone,
                    name = userName,
                    isApprovedUser = isApprovedUser
                )
                
                orderSuccess = true
                onSuccess(order)
            } catch (e: Exception) {
                error = "Failed to place order: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun addField(label: String, value: String) {
        fields.add(Field(label, value))
    }

    fun removeField(i:Int) {
        fields.removeAt(i)
    }
    
    fun incrementQuantity() {
        val maxQuantity = product?.maxQuantity ?: Int.MAX_VALUE
        if (quantity < maxQuantity) {
            quantity++
        }
    }
    
    fun decrementQuantity() {
        if (quantity > 1) {
            quantity--
        }
    }
    
    // Admin: upsert both approved/unapproved variants from UI
    fun upsertBothFromUi(approved: Product?, unapproved: Product?, onSuccess: () -> Unit) {
        if (!isAdmin) return
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val fieldsJson = buildCustomFieldsJson()
                val unApprovedFieldsJson = buildUnApprovedCustomFieldsJson()
                // If requested, copy price from approved to unapproved before save
                val approvedFinal = approved
                val unapprovedFinal = unapproved
                if (isCreateMode) {
                    val newId = productApiService.createBothVariants(
                        id = null,
                        approved = approvedFinal,
                        unapproved = unapprovedFinal,
                        imageBytes = selectedImageBytes,
                        imageFileName = selectedImageFileName,
                        approvedCustomFields = approvedFinal?.let { fieldsJson },
                        unapprovedCustomFields = unapprovedFinal?.let { unApprovedFieldsJson },
                        applyToAll = syncPrices
                    )
                    // Reload preferring approved
                    val both = runCatching { productApiService.getBothVariantsById(newId) }.getOrNull()
                    product = when {
                        both?.approved != null -> Product(
                            id = newId,
                            name = both.approved.name,
                            price = both.approved.price,
                            imageUrl = "",
                            imageBase64 = both.approved.imageBase64,
                            category = both.approved.category,
                            description = both.approved.description,
                            weight = both.approved.weight,
                            purity = both.approved.purity,
                            dimension = both.approved.dimension,
                            maxQuantity = both.approved.maxQuantity
                        )
                        both?.unapproved != null -> Product(
                            id = newId,
                            name = both.unapproved.name,
                            price = both.unapproved.price,
                            imageUrl = "",
                            imageBase64 = both.unapproved.imageBase64,
                            category = both.unapproved.category,
                            description = both.unapproved.description,
                            weight = both.unapproved.weight,
                            purity = both.unapproved.purity,
                            dimension = both.unapproved.dimension,
                            maxQuantity = both.unapproved.maxQuantity
                        )
                        else -> null
                    }
                } else {
                    productApiService.updateBothVariants(
                        id = productId,
                        approved = approvedFinal,
                        unapproved = unapprovedFinal,
                        imageBytes = selectedImageBytes,
                        imageFileName = selectedImageFileName,
                        approvedCustomFields = approvedFinal?.let { fieldsJson },
                        unapprovedCustomFields = unapprovedFinal?.let { unApprovedFieldsJson },
                        applyToAll = syncPrices
                    )
                    // Reload preferring approved
                    val both = runCatching { productApiService.getBothVariantsById(productId) }.getOrNull()
                    product = when {
                        both?.approved != null -> Product(
                            id = productId,
                            name = both.approved.name,
                            price = both.approved.price,
                            imageUrl = "",
                            imageBase64 = both.approved.imageBase64,
                            category = both.approved.category,
                            description = both.approved.description,
                            weight = both.approved.weight,
                            purity = both.approved.purity,
                            dimension = both.approved.dimension,
                            maxQuantity = both.approved.maxQuantity
                        )
                        both?.unapproved != null -> Product(
                            id = productId,
                            name = both.unapproved.name,
                            price = both.unapproved.price,
                            imageUrl = "",
                            imageBase64 = both.unapproved.imageBase64,
                            category = both.unapproved.category,
                            description = both.unapproved.description,
                            weight = both.unapproved.weight,
                            purity = both.unapproved.purity,
                            dimension = both.unapproved.dimension,
                            maxQuantity = both.unapproved.maxQuantity
                        )
                        else -> null
                    }
                }
                onSuccess()
                selectedImageBytes = null
                selectedImageFileName = null
            } catch (e: Exception) {
                error = "Failed to save product: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    private fun buildCustomFieldsJson(): String {
        return try {
            val list = fields.map { FieldDto(it.label, it.value) }
            Json.encodeToString(ListSerializer(FieldDto.serializer()), list)
        } catch (_: Exception) {
            "[]"
        }
    }

    private fun buildUnApprovedCustomFieldsJson(): String {
        return try {
            val list = fieldsUnapproved.map { FieldDto(it.label, it.value) }
            Json.encodeToString(ListSerializer(FieldDto.serializer()), list)
        } catch (_: Exception) {
            "[]"
        }
    }

    private fun setFieldsFromJson(json: String?) {
        if (json.isNullOrBlank()) {
            fields.clear()
            return
        }
        runCatching {
            val dtos = Json.decodeFromString(ListSerializer(FieldDto.serializer()), json)
            fields.clear()
            fields.addAll(dtos.map { Field(it.label, it.value) })
        }.onFailure {
            // On parse failure, keep existing fields or clear
            fields.clear()
        }
    }

    private fun setUnApprovedFieldsFromJson(json: String?) {
        if (json.isNullOrBlank()) {
            fieldsUnapproved.clear()
            return
        }
        runCatching {
            val dtos = Json.decodeFromString(ListSerializer(FieldDto.serializer()), json)
            fieldsUnapproved.clear()
            fieldsUnapproved.addAll(dtos.map { Field(it.label, it.value) })
        }.onFailure {
            // On parse failure, keep existing fields or clear
            fieldsUnapproved.clear()
        }
    }

    fun clearError() {
        error = null
    }
}
