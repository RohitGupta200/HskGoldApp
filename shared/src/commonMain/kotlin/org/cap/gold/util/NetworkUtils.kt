package org.cap.gold.util

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.cap.gold.model.NetworkResult

/**
 * Executes a network request with proper error handling.
 * @param request The suspend function that performs the network request
 * @return NetworkResult with the response or error
 */
suspend inline fun <T> safeApiCall(
    crossinline request: suspend () -> T
): NetworkResult<T> {
    return try {
        NetworkResult.Success(request())
    } catch (e: ClientRequestException) {
        NetworkResult.Error(
            message = e.message ?: "Unknown error",
            code = e.response.status.value
        )
    } catch (e: ServerResponseException) {
        NetworkResult.Error(
            message = e.message ?: "Server error",
            code = e.response.status.value
        )
    } catch (e: Exception) {
        NetworkResult.Error(
            message = e.message ?: "Network error",
            code = -1
        )
    }
}

/**
 * Adds common headers to the request
 */
fun HttpRequestBuilder.addCommonHeaders() {
    headers {
        append(HttpHeaders.ContentType, ContentType.Application.Json)
        append(HttpHeaders.Accept, ContentType.Application.Json)
        // Add any other common headers here
    }
}
