package org.cap.gold.auth

import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import org.cap.gold.model.NetworkResult

/**
 * Handles authentication errors and provides user-friendly error messages.
 */
object AuthErrorHandler {
    /**
     * Authentication error types with default messages.
     */
    enum class AuthErrorType(val message: String) {
        GENERIC("An authentication error occurred"),
        UNAUTHORIZED("Authentication failed. Please check your credentials."),
        USER_NOT_FOUND("User not found. Please check your email."),
        USER_EXISTS("An account with this email already exists."),
        TOO_MANY_REQUESTS("Too many requests. Please try again later."),
        INVALID_REQUEST("Invalid request. Please check your input."),
        SERVER_ERROR("Server error. Please try again later."),
        NETWORK_ERROR("Network error. Please check your connection."),
        INVALID_INPUT("Invalid input. Please check your details and try again."),
        INVALID_STATE("Invalid application state. Please try again."),
        UNKNOWN("An unknown error occurred.")
    }

    /**
     * Processes an authentication error and returns an error type.
     * @param error The error that occurred
     * @return The corresponding AuthErrorType
     */
    fun handleAuthError(error: Throwable): AuthErrorType {
        return when (error) {
            is ClientRequestException -> {
                when (error.response.status) {
                    HttpStatusCode.Unauthorized -> AuthErrorType.UNAUTHORIZED
                    HttpStatusCode.NotFound -> AuthErrorType.USER_NOT_FOUND
                    HttpStatusCode.Conflict -> AuthErrorType.USER_EXISTS
                    HttpStatusCode.TooManyRequests -> AuthErrorType.TOO_MANY_REQUESTS
                    HttpStatusCode.BadRequest -> AuthErrorType.INVALID_REQUEST
                    else -> AuthErrorType.GENERIC
                }
            }
            is ServerResponseException -> AuthErrorType.SERVER_ERROR
            is IOException -> AuthErrorType.NETWORK_ERROR
            is IllegalArgumentException -> AuthErrorType.INVALID_INPUT
            is IllegalStateException -> AuthErrorType.INVALID_STATE
            else -> AuthErrorType.UNKNOWN
        }
    }
    
    /**
     * Handles a network result and returns either the data or an error type.
     * @param result The network result to process
     * @param onSuccess Callback for successful result
     * @param onError Callback for error result with an error type
     */
    fun <T> handleResult(
        result: NetworkResult<T>,
        onSuccess: (T) -> Unit,
        onError: (AuthErrorType) -> Unit
    ) {
        when (result) {
            is NetworkResult.Success -> {
                if (result.data != null) {
                    onSuccess(result.data)
                } else {
                    onError(AuthErrorType.UNKNOWN)
                }
            }
            is NetworkResult.Error -> {
                val errorType = when (result.code) {
                    HttpStatusCode.Unauthorized.value -> AuthErrorType.UNAUTHORIZED
                    HttpStatusCode.NotFound.value -> AuthErrorType.USER_NOT_FOUND
                    HttpStatusCode.Conflict.value -> AuthErrorType.USER_EXISTS
                    HttpStatusCode.TooManyRequests.value -> AuthErrorType.TOO_MANY_REQUESTS
                    in 400..499 -> AuthErrorType.INVALID_REQUEST
                    in 500..599 -> AuthErrorType.SERVER_ERROR
                    else -> AuthErrorType.UNKNOWN
                }
                onError(errorType)
            }
            is NetworkResult.Loading -> {
                // Handle loading state if needed
            }
        }
    }
}
