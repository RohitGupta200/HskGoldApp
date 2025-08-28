package org.cap.gold.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.cap.gold.model.User

interface UsersApiService {
    suspend fun listUsers(pageToken: String? = null, search: String? = null): UsersPage
    suspend fun updateUserRole(userId: String, role: Int): User
}

@Serializable
private data class ServerUsersPageResponse(
    val users: List<ServerUser>,
    val nextPageToken: String? = null
)

@Serializable
private data class ServerUser(
    val id: String,
    val phoneNumber: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val name: String? = null,
    val role: Int = 3,
    val shopName: String? = null,
    val createdAt: Long = 0,
    val lastLogin: Long = 0
)

@Serializable
private data class RoleUpdateRequest(val role: Int)

data class UsersPage(
    val users: List<User>,
    val nextPageToken: String?
)

class UsersApiServiceImpl(
    private val client: HttpClient
) : UsersApiService {
    override suspend fun listUsers(pageToken: String?, search: String?): UsersPage {
        val resp: ServerUsersPageResponse = client.get("api/users") {
            if (!pageToken.isNullOrBlank()) parameter("pageToken", pageToken)
            if (!search.isNullOrBlank()) parameter("search", search)
        }.body()
        return UsersPage(
            users = resp.users.map { it.toUser() },
            nextPageToken = resp.nextPageToken
        )
    }

    override suspend fun updateUserRole(userId: String, role: Int): User {
        val updated: ServerUser = client.patch("api/users/$userId/role") {
            contentType(ContentType.Application.Json)
            setBody(RoleUpdateRequest(role))
        }.body()
        return updated.toUser()
    }
}

private fun ServerUser.toUser(): User = User(
    id = id,
    phoneNumber = phoneNumber,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isEmailVerified = isEmailVerified,
    name = name ?: displayName,
    role = role,
    shopName = shopName,
    createdAt = createdAt,
    lastLogin = lastLogin
)
