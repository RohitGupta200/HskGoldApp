package org.cap.gold.controllers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ListUsersPage
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
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 100

                // Firebase Admin SDK uses cursor-based pagination; emulate page/pageSize
                val users = mutableListOf<UserListItem>()
                var currentPage: ListUsersPage = firebaseAuth.listUsers(null)
                while (true) {
                    currentPage.iterateAll().forEach { userRecord ->
                        users += userRecord.toUserListItem()
                    }
                    if (!currentPage.hasNextPage() || users.size >= (page + 1) * pageSize) break
                    currentPage = currentPage.getNextPage()
                }

                val fromIndex = (page * pageSize).coerceAtMost(users.size)
                val toIndex = (fromIndex + pageSize).coerceAtMost(users.size)
                val pageItems = users.subList(fromIndex, toIndex)

                // Map to API model expected by client (shared org.cap.gold.model.User)
                val apiUsers = pageItems.map { it.toApiUser() }
                call.respond(HttpStatusCode.OK, apiUsers)
            }

            patch("/{id}/role") {
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
