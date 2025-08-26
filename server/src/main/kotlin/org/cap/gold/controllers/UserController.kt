package org.cap.gold.controllers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ListUsersPage
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.cap.gold.models.User as ServerUser
import org.cap.gold.models.UserResponse

class UserController(
    private val firebaseAuth: FirebaseAuth
) {
    fun Route.userRoutes() {
        route("/users") {
            get {
                // Extract user id from JWT (subject claim)
                val userId = call.principal<JWTPrincipal>()?.payload?.subject
                // Optionally enforce presence (should always be present due to authenticate("auth-jwt"))
                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid token"))
                    return@get
                }
                // Query params
                val pageSize = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 100)
                    .coerceIn(1, 1000) // Firebase max is 1000
                val pageToken = call.request.queryParameters["pageToken"]?.toString()
                val search = call.request.queryParameters["search"]?.toString() ?: ""

                // Note: search is accepted but not applied server-side (client will handle it)

                // Use Firebase Admin listUsers with pageToken
                // Note: Java Admin SDK does not expose maxResults in the public API; it returns up to 1000 users per page.
                val page: ListUsersPage = firebaseAuth.listUsers(pageToken)

                val usersBatch = page.values.map { it.toUserListItem().toApiUser() }
                val nextTokenRaw = page.nextPageToken
                val nextToken = if (nextTokenRaw.isNullOrBlank()) null else nextTokenRaw

                call.respond(
                    HttpStatusCode.OK,
                    UsersPageResponse(
                        users = usersBatch,
                        nextPageToken = nextToken
                    )
                )
            }

            patch("/{id}/role") {
                // Extract user id from JWT (subject claim)
                val requesterId = call.principal<JWTPrincipal>()?.payload?.subject
                if (requesterId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid token"))
                    return@patch
                }
                val userId = call.parameters["id"]
                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user id"))
                    return@patch
                }
                val body = call.receive<RoleUpdateRequest>()
                val newRole = body.role

                // Get existing claims and update
                val userRecord = firebaseAuth.getUser(userId)
                val currentClaims = (userRecord.customClaims ?: emptyMap()).toMutableMap()
                currentClaims["role"] = newRole
                firebaseAuth.setCustomUserClaims(userId, currentClaims)

                // Return updated user in API shape
                val refreshed = firebaseAuth.getUser(userId)
                val updated = refreshed.toUserListItem().toApiUser()
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}

@Serializable
private data class RoleUpdateRequest(val role: Int)

@Serializable
private data class ApiUser(
    val id: String,
    val phoneNumber: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val name: String? = null,
    val role: Int = 3,
    val createdAt: Long = 0,
    val lastLogin: Long = 0
)

private data class UserListItem(
    val id: String,
    val phoneNumber: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    val createdAt: Long,
    val lastLogin: Long,
    val role: Int
)

@Serializable
private data class UsersPageResponse(
    val users: List<ApiUser>,
    val nextPageToken: String? = null
)

private fun com.google.firebase.auth.UserRecord.toUserListItem(): UserListItem {
    val role = (customClaims?.get("role") as? Number)?.toInt() ?: 3
    return UserListItem(
        id = uid,
        phoneNumber = phoneNumber ?: "",
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        emailVerified = isEmailVerified,
        createdAt = userMetadata?.creationTimestamp ?: 0,
        lastLogin = userMetadata?.lastSignInTimestamp ?: 0,
        role = role
    )
}

private fun UserListItem.toApiUser(): ApiUser = ApiUser(
    id = id,
    phoneNumber = phoneNumber,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isEmailVerified = emailVerified,
    name = displayName,
    role = role,
    createdAt = createdAt,
    lastLogin = lastLogin
)
