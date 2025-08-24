package org.cap.gold.data.repository

import org.cap.gold.data.model.Product
import org.cap.gold.data.remote.ProductApiService
import org.cap.gold.data.network.NetworkResponse
import org.cap.gold.data.network.handleApiResponse

/**
 * Repository for product-related operations
 */
interface ProductRepository {
    /**
     * Get all approved products
     */
    suspend fun getApprovedProducts(): NetworkResponse<List<Product>>
    
    /**
     * Get all unapproved products (admin only)
     */
    suspend fun getUnapprovedProducts(): NetworkResponse<List<Product>>
    
    /**
     * Get product by ID
     */
    suspend fun getProductById(id: String): NetworkResponse<Product>
    
    /**
     * Get unapproved product by ID (admin only)
     */
    suspend fun getUnapprovedProductById(id: String): NetworkResponse<Product>

    /**
     * Create products
     */
    suspend fun createApprovedProduct(product: Product): NetworkResponse<Product>
    suspend fun createUnapprovedProduct(product: Product): NetworkResponse<Product>

    /**
     * Update products
     */
    suspend fun updateApprovedProduct(id: String, product: Product): NetworkResponse<Product>
    suspend fun updateUnapprovedProduct(id: String, product: Product): NetworkResponse<Product>

    /**
     * Delete products
     */
    suspend fun deleteApprovedProduct(id: String): NetworkResponse<Boolean>
    suspend fun deleteUnapprovedProduct(id: String): NetworkResponse<Boolean>
}

/**
 * Implementation of [ProductRepository] that uses [ProductApiService]
 */
class ProductRepositoryImpl(
    private val apiService: ProductApiService
) : ProductRepository {
    
    override suspend fun getApprovedProducts(): NetworkResponse<List<Product>> {
        return handleApiResponse {
            apiService.getApprovedProducts()
        }
    }
    
    override suspend fun getUnapprovedProducts(): NetworkResponse<List<Product>> {
        return handleApiResponse {
            apiService.getUnapprovedProducts()
        }
    }
    
    override suspend fun getProductById(id: String): NetworkResponse<Product> {
        return handleApiResponse {
            apiService.getProductById(id)
        }
    }
    
    override suspend fun getUnapprovedProductById(id: String): NetworkResponse<Product> {
        return handleApiResponse {
            apiService.getUnapprovedProductById(id)
        }
    }

    override suspend fun createApprovedProduct(product: Product): NetworkResponse<Product> {
        return handleApiResponse {
            apiService.createApprovedProduct(product)
        }
    }

    override suspend fun createUnapprovedProduct(product: Product): NetworkResponse<Product> {
        return handleApiResponse {
            apiService.createUnapprovedProduct(product)
        }
    }

    override suspend fun updateApprovedProduct(id: String, product: Product): NetworkResponse<Product> {
        return handleApiResponse {
            apiService.updateApprovedProduct(id, product)
        }
    }

    override suspend fun updateUnapprovedProduct(id: String, product: Product): NetworkResponse<Product> {
        return handleApiResponse {
            apiService.updateUnapprovedProduct(id, product)
        }
    }

    override suspend fun deleteApprovedProduct(id: String): NetworkResponse<Boolean> {
        return handleApiResponse {
            apiService.deleteApprovedProduct(id)
        }
    }

    override suspend fun deleteUnapprovedProduct(id: String): NetworkResponse<Boolean> {
        return handleApiResponse {
            apiService.deleteUnapprovedProduct(id)
        }
    }
}
