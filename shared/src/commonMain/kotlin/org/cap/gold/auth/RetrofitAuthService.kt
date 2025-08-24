//package org.cap.gold.auth
//
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import org.cap.gold.api.ApiClient
//import org.cap.gold.api.AuthApiService
//import org.cap.gold.api.UserResponse
//import org.cap.gold.model.NetworkResult
//import org.cap.gold.util.safeApiCall
//import org.cap.gold.util.validatePhoneNumber
//import org.cap.gold.util.validatePassword
//
///**
// * Implementation of [AuthService] using Ktor for API communication.
// * This is a temporary implementation that should be replaced with KtorAuthService.
// */
//@Deprecated("Use KtorAuthService instead")
//class RetrofitAuthService : AuthService {
//    private val _authState = MutableStateFlow<User?>(null)
//    private val _isLoading = MutableStateFlow(false)
//    private val _error = MutableStateFlow<String?>(null)
//
//    override val currentUser: User?
//        get() = _authState.value
//
//    override val authState: Flow<User?> = _authState.asStateFlow()
//    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
//    override val error: StateFlow<String?> = _error.asStateFlow()
//
//    private fun clearError() {
//        _error.value = null
//    }
//
//    private fun setError(message: String) {
//        _error.value = message
//    }
//
//    override suspend fun signInWithPhone(phoneNumber: String, password: String): AuthResult<User> {
//        // Validate phone number
//        validatePhoneNumber(phoneNumber).let { validation ->
//            if (!validation.success) {
//                return AuthResult.Error(validation.errorMessage ?: "Invalid phone number")
//            }
//        }
//
//        // Validate password
//        validatePassword(password).let { validation ->
//            if (!validation.success) {
//                return AuthResult.Error(validation.errorMessage ?: "Invalid password")
//            }
//        }
//
//        _isLoading.value = true
//        clearError()
//
//        return safeApiCall {
//            // This is a placeholder - replace with actual API call
//            // val response = client.post("signin/phone") {
//            //     setBody(PhoneSignInRequest(phoneNumber, password))
//            // }
//            //
//            // val responseBody = response.body<Map<String, String>>()
//            val user = User(
//                id = "temp-user-id", // Replace with actual user ID from response
//                phoneNumber = phoneNumber,
//                displayName = null
//            )
//
//            _authState.value = user
//            AuthResult.Success(user)
//        }.let { result ->
//            when (result) {
//                is NetworkResult.Success -> result.data
//                is NetworkResult.Error -> {
//                    setError(result.message)
//                    AuthResult.Error(result.message)
//                }
//                else -> AuthResult.Error("Unknown error occurred")
//            }
//        }.also {
//            _isLoading.value = false
//        }
//    }
//
//    override suspend fun createUserWithPhone(
//        phoneNumber: String,
//        password: String,
//        displayName: String?
//    ): AuthResult<User> {
//        // Validate phone number
//        validatePhoneNumber(phoneNumber).let { validation ->
//            if (!validation.success) {
//                return AuthResult.Error(validation.errorMessage ?: "Invalid phone number")
//            }
//        }
//
//        // Validate password
//        validatePassword(password).let { validation ->
//            if (!validation.success) {
//                return AuthResult.Error(validation.errorMessage ?: "Invalid password")
//            }
//        }
//
//        _isLoading.value = true
//        clearError()
//
//        return safeApiCall {
//            // This is a placeholder - replace with actual API call
//            // val response = client.post("signup/phone") {
//            //     setBody(PhoneSignUpRequest(phoneNumber, password, displayName))
//            // }
//            //
//            // val responseBody = response.body<Map<String, String>>()
//            val user = User(
//                id = "temp-user-id", // Replace with actual user ID from response
//                phoneNumber = phoneNumber,
//                displayName = displayName
//            )
//
//            _authState.value = user
//            AuthResult.Success(user)
//        }.let { result ->
//            when (result) {
//                is NetworkResult.Success -> result.data
//                is NetworkResult.Error -> {
//                    setError(result.message)
//                    AuthResult.Error(result.message)
//                }
//                else -> AuthResult.Error("Unknown error occurred")
//            }
//        }.also {
//            _isLoading.value = false
//        }
//    }
//
//    override suspend fun signOut() {
//        _authState.value = null
//        ApiClient.clearAuthToken()
//    }
//
//    override suspend fun sendPasswordResetSms(phoneNumber: String): AuthResult<Unit> {
//        // Validate phone number
//        validatePhoneNumber(phoneNumber).let { validation ->
//            if (!validation.success) {
//                return AuthResult.Error(validation.errorMessage ?: "Invalid phone number")
//            }
//        }
//
//        _isLoading.value = true
//        clearError()
//
//        return safeApiCall {
//            // This is a placeholder - replace with actual API call
//            // client.post("reset-password/phone") {
//            //     setBody(PasswordResetRequest(phoneNumber))
//            // }
//            AuthResult.Success(Unit)
//        }.let { result ->
//            when (result) {
//                is NetworkResult.Success -> result.data
//                is NetworkResult.Error -> {
//                    setError(result.message)
//                    AuthResult.Error(result.message)
//                }
//                else -> AuthResult.Error("Failed to send password reset SMS")
//            }
//        }.also {
//            _isLoading.value = false
//        }
//    }
//
//    override suspend fun updateProfile(displayName: String?, photoUrl: String?): AuthResult<Unit> {
//        _isLoading.value = true
//        clearError()
//
//        return safeApiCall {
//            // This is a placeholder - replace with actual API call
//            // client.put("profile") {
//            //     setBody(mapOf(
//            //         "displayName" to displayName,
//            //         "photoUrl" to photoUrl
//            //     ))
//            // }
//
//            // Update local user state
//            _authState.value?.let { currentUser ->
//                _authState.value = currentUser.copy(
//                    displayName = displayName ?: currentUser.displayName,
//                    photoUrl = photoUrl ?: currentUser.photoUrl
//                )
//            }
//
//            AuthResult.Success(Unit)
//        }.let { result ->
//            when (result) {
//                is NetworkResult.Success -> result.data
//                is NetworkResult.Error -> {
//                    setError(result.message)
//                    AuthResult.Error(result.message)
//                }
//                else -> AuthResult.Error("Failed to update profile")
//            }
//        }.also {
//            _isLoading.value = false
//        }
//    }
//
//    companion object {
//        /**
//         * Creates a new instance of [RetrofitAuthService].
//         */
//        fun create(): RetrofitAuthService = RetrofitAuthService()
//    }
//}
