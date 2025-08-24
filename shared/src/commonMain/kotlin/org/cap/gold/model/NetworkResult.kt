package org.cap.gold.model

/**
 * Sealed class representing the result of a network operation.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>() 
    data class Error(
        val message: String,
        val code: Int = -1
    ) : NetworkResult<Nothing>()
    
    object Loading : NetworkResult<Nothing>() {
        override fun toString(): String = "Loading..."
    }
    
    /**
     * Returns the data if the result is Success, or null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data
    
    /**
     * Returns true if the result is Success.
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Returns true if the result is Error.
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Returns true if the result is Loading.
     */
    fun isLoading(): Boolean = this is Loading
}
