package org.cap.gold.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.cap.gold.data.model.Category

interface CategoryApiService {
    suspend fun getAll(): List<Category>
    suspend fun create(name: String): Category
    suspend fun delete(id: String): Boolean
}

class CategoryApiServiceImpl(
    private val client: HttpClient
) : CategoryApiService {
    @Serializable
    private data class CreateCategoryRequest(val name: String)

    override suspend fun getAll(): List<Category> =
        client.get("api/category/all").body()

    override suspend fun create(name: String): Category =
        client.post("api/category/create") {
            contentType(ContentType.Application.Json)
            setBody(CreateCategoryRequest(name.trim()))
        }.body()

    override suspend fun delete(id: String): Boolean =
        client.delete("api/category/delete") {
            url { parameters.append("id", id) }
        }.status.value in 200..299
}
