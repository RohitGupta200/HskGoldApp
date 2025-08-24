package org.cap.gold.data.repository

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import org.cap.gold.network.NetworkClient
import org.cap.gold.model.User
import org.cap.gold.util.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Repository for user management operations
 */
interface UserRepository {
    /**
     * Fetches a paginated list of all users
     * @param page The page number (0-based)
     * @param pageSize Number of users per page
     * @return Result containing the list of users or an error
     */
    suspend fun getUsers(page: Int = 0, pageSize: Int = 100): Result<List<User>>
    
    /**
     * Updates a user's role
     * @param userId The ID of the user to update
     * @param newRole The new role value (0=admin, 1=Approved, 2=UnApproved, 3=regular)
     * @return Result containing the updated user or an error
     */
    suspend fun updateUserRole(userId: String, newRole: Int): Result<User>
}

/**
 * Implementation of UserRepository that communicates with the backend API
 */
class UserRepositoryImpl : UserRepository, KoinComponent {
    private val network by inject<NetworkClient>()
    
    override suspend fun getUsers(page: Int, pageSize: Int): Result<List<User>> {
        return try {
            val httpResp: HttpResponse = network.client.get("/api/users") {
                parameter("page", page)
                parameter("pageSize", pageSize)
            }
            if (httpResp.status.value !in 200..299) {
                val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to fetch users" }
                return Result.Error(msg.ifBlank { "Failed to fetch users" })
            }
            try {
                val users: List<User> = httpResp.body()
                Result.Success(users)
            } catch (_: Exception) {
                // Fallback wrapped shape
                @kotlinx.serialization.Serializable
                data class Wrapped<T>(val users: List<T>? = null)
                val wrapped: Wrapped<User> = httpResp.body()
                Result.Success(wrapped.users ?: emptyList())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch users")
        }
    }
    
    override suspend fun updateUserRole(userId: String, newRole: Int): Result<User> {
        return try {
            val httpResp: HttpResponse = network.client.patch("/api/users/$userId/role") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("role" to newRole))
            }
            if (httpResp.status.value !in 200..299) {
                val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to update user role" }
                return Result.Error(msg.ifBlank { "Failed to update user role" })
            }
            try {
                val updatedUser: User = httpResp.body()
                Result.Success(updatedUser)
            } catch (_: Exception) {
                // Fallback wrapper { user: ... }
                @kotlinx.serialization.Serializable
                data class Wrapped(val user: User? = null)
                val wrapped: Wrapped = httpResp.body()
                val u = wrapped.user ?: return Result.Error("Invalid user response")
                Result.Success(u)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update user role")
        }
    }
}
