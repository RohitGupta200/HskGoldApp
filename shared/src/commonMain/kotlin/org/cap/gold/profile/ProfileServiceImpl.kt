package org.cap.gold.profile

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.cap.gold.model.User
import org.cap.gold.network.NetworkClient

class ProfileServiceImpl(
    private val baseUrl: String,
    private val network: NetworkClient
) : ProfileService {

    @Serializable
    private data class ChangePasswordRequest(val currentPassword: String? = null, val newPassword: String)

    @Serializable
    private data class ChangePhoneRequest(val password: String? = null, val newPhone: String)

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

    override suspend fun changePassword(newPassword: String) {
        val resp = network.client.post("$baseUrl/api/auth/password/change") {
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(newPassword = newPassword))
        }
        if (resp.status.value !in 200..299) {
            val msg = try { resp.bodyAsText() } catch (_: Exception) { "Password change failed" }
            throw IllegalStateException(msg.ifBlank { "Password change failed" })
        }
    }

    override suspend fun changePhone(newPhone: String): User {
        val httpResp: HttpResponse = network.client.post("$baseUrl/api/auth/phone/change") {
            contentType(ContentType.Application.Json)
            setBody(ChangePhoneRequest(newPhone = newPhone))
        }
        if (httpResp.status.value !in 200..299) {
            val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Phone change failed" }
            throw IllegalStateException(msg.ifBlank { "Phone change failed" })
        }
        // Try expected response, fallback to user directly
        return try {
            val resp: PhoneChangeResponse = httpResp.body()
            User(
                id = resp.userId,
                phoneNumber = resp.phoneNumber,
                email = resp.email,
                displayName = resp.displayName,
                isEmailVerified = false
            )
        } catch (_: Exception) {
            // If server returns User directly (e.g., /auth/me shape)
            httpResp.body<User>()
        }
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
