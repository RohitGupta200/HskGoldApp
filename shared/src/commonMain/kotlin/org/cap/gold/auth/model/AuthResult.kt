package org.cap.gold.auth.model

import kotlinx.serialization.Serializable

/**
 * A sealed class representing the result of an authentication operation.
 * @param T The type of data returned on success
 */
@Serializable
sealed class AuthResult<out T> {
    /**
     * Represents a successful authentication operation.
     * @property data The data returned on success
     */
    @Serializable
    data class Success<T>(val data: T) : AuthResult<T>()

    /**
     * Represents a failed authentication operation.
     * @property message A message describing the error
     * @property exception The exception that caused the failure, if any
     */
    @Serializable
    data class Error(
        val message: String,
        val exception: String? = null
    ) : AuthResult<Nothing>() {
        constructor(throwable: Throwable) : this(
            message = throwable.message ?: "An unknown error occurred",
            exception = throwable::class.simpleName
        )
    }

    object Loading : AuthResult<Nothing>()
}
