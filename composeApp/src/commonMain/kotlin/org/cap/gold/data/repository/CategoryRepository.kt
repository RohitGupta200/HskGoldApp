package org.cap.gold.data.repository

import org.cap.gold.data.model.Category
import org.cap.gold.data.network.NetworkResponse
import org.cap.gold.data.network.handleApiResponse
import org.cap.gold.data.remote.CategoryApiService

interface CategoryRepository {
    suspend fun getAll(): NetworkResponse<List<Category>>
    suspend fun create(name: String): NetworkResponse<Category>
    suspend fun delete(id: String): NetworkResponse<Boolean>
}

class CategoryRepositoryImpl(
    private val api: CategoryApiService
) : CategoryRepository {
    override suspend fun getAll(): NetworkResponse<List<Category>> =
        handleApiResponse { api.getAll() }

    override suspend fun create(name: String): NetworkResponse<Category> =
        handleApiResponse { api.create(name) }

    override suspend fun delete(id: String): NetworkResponse<Boolean> =
        handleApiResponse { api.delete(id) }
}
