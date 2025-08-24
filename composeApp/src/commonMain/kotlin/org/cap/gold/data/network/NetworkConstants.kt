package org.cap.gold.data.network

object NetworkConstants {
    // Update this with your actual backend URL
    const val BASE_URL = "http://your-backend-url.com/api/v1"
    
    // Headers
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_BEARER = "Bearer"
    
    // Timeouts
    const val CONNECT_TIMEOUT_MS = 30_000L
    const val SOCKET_TIMEOUT_MS = 30_000L
    
    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
}
