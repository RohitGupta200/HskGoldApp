package org.cap.gold.data.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode

/**
 * A sealed class that represents different states of a network request.
 */
sealed class NetworkResponse<out T> {
    data class Success<T>(val data: T) : NetworkResponse<T>()
    data class Error(val message: String) : NetworkResponse<Nothing>()
    object Loading : NetworkResponse<Nothing>() {
        override fun toString(): String = "Loading"
    }
}

/**
 * Helper function to handle API responses and convert them to NetworkResponse type.
 */
suspend inline fun <T> handleApiResponse(
    crossinline apiCall: suspend () -> T
): NetworkResponse<T> {
    return try {
        NetworkResponse.Success(apiCall())
    } catch (e: ClientRequestException) {
        when (e.response.status) {
            HttpStatusCode.Unauthorized -> NetworkResponse.Error("Unauthorized access. Please login again.")
            HttpStatusCode.Forbidden -> NetworkResponse.Error("You don't have permission to perform this action.")
            HttpStatusCode.NotFound -> NetworkResponse.Error("Resource not found.")
            HttpStatusCode.BadRequest -> {
                val errorMessage = e.errorBody<ErrorResponse>()?.error ?: "Invalid request"
                NetworkResponse.Error(errorMessage)
            }
            else -> NetworkResponse.Error("Network error: ${e.message}")
        }
    } catch (e: ServerResponseException) {
        NetworkResponse.Error("Server error: ${e.message}")
    } catch (e: Exception) {
        NetworkResponse.Error("An unexpected error occurred: ${e.message}")
    }
}

/**
 * Extension function to parse error response body.
 */
suspend inline fun <reified T> ClientRequestException.errorBody(): T? {
    return try {
        response.body()
    } catch (e: Exception) {
        null
    }
}

/**
 * Data class for error responses from the API.
 */
@kotlinx.serialization.Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null,
    val status: Int? = null
)
