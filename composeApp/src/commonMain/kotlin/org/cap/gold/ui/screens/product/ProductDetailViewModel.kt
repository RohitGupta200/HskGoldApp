package org.cap.gold.ui.screens.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cap.gold.data.model.Order
import org.cap.gold.data.model.OrderStatus
import org.cap.gold.data.model.Product
import org.cap.gold.data.remote.ProductApiService

class ProductDetailViewModel(
    private val productApiService: ProductApiService,
    private val productId: String,
    val isAdmin: Boolean,
    val isApprovedUser: Boolean
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var product by mutableStateOf<Product?>(null)
    var orderSuccess by mutableStateOf(false)
    var showOrderDialog by mutableStateOf(false)
    var quantity by mutableStateOf(1)
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
                weight = 0.0,
                purity = "",
                dimension = "",
                maxQuantity = 1
            )
        } else {
            loadProduct()
        }
    }
    
    fun loadProduct() {
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                product = if (isAdmin) {
                    // Admin: fetch both and prefer approved if present
                    val both = try { productApiService.getBothVariantsById(productId) } catch (_: Exception) { null }
                    when {
                        both == null -> null
                        both.approved != null -> Product(
                            id = productId,
                            name = "", // name not part of payload yet
                            price = both.approved.price,
                            imageUrl = "",
                            category = both.approved.category,
                            description = "",
                            weight = both.approved.weight,
                            purity = both.approved.purity,
                            dimension = both.approved.dimension,
                            maxQuantity = both.approved.maxQuantity
                        )
                        both.unapproved != null -> Product(
                            id = productId,
                            name = "",
                            price = both.unapproved.price,
                            imageUrl = "",
                            category = both.unapproved.category,
                            description = "",
                            weight = both.unapproved.weight,
                            purity = both.unapproved.purity,
                            dimension = both.unapproved.dimension,
                            maxQuantity = both.unapproved.maxQuantity
                        )
                        else -> null
                    }
                } else if (isApprovedUser) {
                    productApiService.getProductById(productId)
                } else {
                    productApiService.getUnapprovedProductById(productId)
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
                if (isCreateMode) {
                    // Create via both-variants API; send only one side to allow copy
                    val newId = productApiService.createBothVariants(
                        id = null,
                        approved = updatedProduct,
                        unapproved = null
                    )
                    // Load created product (approved preferred)
                    product = productApiService.getProductById(newId)
                } else {
                    // Update via both-variants API; here we send approved variant only by default
                    productApiService.updateBothVariants(
                        id = productId,
                        approved = updatedProduct,
                        unapproved = null
                    )
                    // Refresh current product
                    product = productApiService.getProductById(productId)
                }
                onSuccess()
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
                    name = userName
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
                if (isCreateMode) {
                    val newId = productApiService.createBothVariants(
                        id = null,
                        approved = approved,
                        unapproved = unapproved
                    )
                    // Reload preferring approved
                    val both = runCatching { productApiService.getBothVariantsById(newId) }.getOrNull()
                    product = when {
                        both?.approved != null -> Product(
                            id = newId,
                            name = "",
                            price = both.approved.price,
                            imageUrl = "",
                            category = both.approved.category,
                            description = "",
                            weight = both.approved.weight,
                            purity = both.approved.purity,
                            dimension = both.approved.dimension,
                            maxQuantity = both.approved.maxQuantity
                        )
                        both?.unapproved != null -> Product(
                            id = newId,
                            name = "",
                            price = both.unapproved.price,
                            imageUrl = "",
                            category = both.unapproved.category,
                            description = "",
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
                        approved = approved,
                        unapproved = unapproved
                    )
                    // Reload preferring approved
                    val both = runCatching { productApiService.getBothVariantsById(productId) }.getOrNull()
                    product = when {
                        both?.approved != null -> Product(
                            id = productId,
                            name = "",
                            price = both.approved.price,
                            imageUrl = "",
                            category = both.approved.category,
                            description = "",
                            weight = both.approved.weight,
                            purity = both.approved.purity,
                            dimension = both.approved.dimension,
                            maxQuantity = both.approved.maxQuantity
                        )
                        both?.unapproved != null -> Product(
                            id = productId,
                            name = "",
                            price = both.unapproved.price,
                            imageUrl = "",
                            category = both.unapproved.category,
                            description = "",
                            weight = both.unapproved.weight,
                            purity = both.unapproved.purity,
                            dimension = both.unapproved.dimension,
                            maxQuantity = both.unapproved.maxQuantity
                        )
                        else -> null
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                error = "Failed to save product: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearError() {
        error = null
    }
}
