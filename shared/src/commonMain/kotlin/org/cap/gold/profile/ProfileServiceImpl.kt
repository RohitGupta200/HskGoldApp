package org.cap.gold.profile

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.cap.gold.auth.AuthService
import org.cap.gold.model.User
import org.cap.gold.network.NetworkClient

class ProfileServiceImpl(
    private val baseUrl: String,
    private val network: NetworkClient
) : ProfileService {

    @Serializable
    private data class ChangePasswordRequest(val currentPassword: String? = null, val newPassword: String)

    @Serializable
    private data class UpdateMeRequest(
        val displayName: String? = null,
        val email: String? = null,
        val phoneNumber: String? = null,
        val shopName: String? = null,
        val currentPassword: String? = null
    )

    @Serializable
    private data class PhoneChangeResponse(
        val token: String,
        val userId: String,
        val email: String? = null,
        val phoneNumber: String,
        val displayName: String? = null,
        val isDisabled: Boolean = false,
        val isNewUser: Boolean = false
    )

    override suspend fun changePassword(currentPassword: String?, newPassword: String) {
        val resp = network.client.post("$baseUrl/api/auth/password/change") {
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword))
        }
        if (resp.status.value !in 200..299) {
            val msg = try { resp.bodyAsText() } catch (_: Exception) { "Password change failed" }
            throw IllegalStateException(msg.ifBlank { "Password change failed" })
        }
    }

    override suspend fun changePhone(newPhone: String,password: String): User {
        val httpResp: HttpResponse = network.client.put("$baseUrl/api/auth/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateMeRequest(phoneNumber = newPhone,currentPassword = password))
        }
        if (httpResp.status.value !in 200..299) {
            val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Phone change failed" }
            throw IllegalStateException(msg.ifBlank { "Phone change failed" })
        }
        // Refresh and return canonical user
        return getMe()
    }

    override suspend fun changeEmail(newEmail: String,password: String): User {
        val httpResp: HttpResponse = network.client.put("$baseUrl/api/auth/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateMeRequest(shopName = newEmail,currentPassword = password))
        }
        if (httpResp.status.value !in 200..299) {
            val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Shop change failed" }
            throw IllegalStateException(msg.ifBlank { "Shop change failed" })
        }
        return getMe()
    }

    override suspend fun changeName(newName: String,password: String): User {
        val httpResp: HttpResponse = network.client.put("$baseUrl/api/auth/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateMeRequest(displayName = newName,currentPassword = password))
        }
        if (httpResp.status.value !in 200..299) {
            val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Name change failed" }
            throw IllegalStateException(msg.ifBlank { "Name change failed" })
        }
        return getMe()
    }

    override suspend fun getMe(): User {
        // Prefer using existing auth/me if available
        val httpResp = network.client.get("$baseUrl/api/auth/me")
        if (httpResp.status.value !in 200..299) {
            val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Failed to load profile" }
            throw IllegalStateException(msg.ifBlank { "Failed to load profile" })
        }
        return try { httpResp.body<User>() } catch (_: Exception) {
            // Some backends may wrap user in { user, tokens } during auth; try to extract
            @kotlinx.serialization.Serializable
            data class Wrapped(val user: User? = null)
            httpResp.body<Wrapped>().user ?: throw IllegalStateException("Invalid profile response")
        }
    }
}

