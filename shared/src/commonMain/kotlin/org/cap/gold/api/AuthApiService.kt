//package org.cap.gold.api
//
//import org.cap.gold.auth.model.AuthResponse
//import org.cap.gold.auth.model.PhoneSignInRequest
//import org.cap.gold.auth.model.PhoneSignUpRequest
//import retrofit2.Response
//import retrofit2.http.Body
//import retrofit2.http.POST
//import retrofit2.http.GET
//import retrofit2.http.Header
//
///**
// * Retrofit service interface for authentication API endpoints.
// */
//interface AuthApiService {
//    /**
//     * Authenticate a user with email and password
//     * @param request The authentication request containing email and password
//     * @return Response containing the authentication result
//     */
//    @POST("auth/signIn")
//    suspend fun signIn(@Body request: AuthRequest): Response<AuthResponse>
//
//    /**
//     * Register a new user with email and password
//     * @param request The registration request containing email and password
//     * @return Response containing the authentication result
//     */
//    @POST("auth/signUp")
//    suspend fun signUp(@Body request: AuthRequest): Response<AuthResponse>
//
//    /**
//     * Get the current authenticated user's profile
//     * @return Response containing the user's profile
//     */
//    @POST("auth/me")
//    suspend fun getCurrentUser(): Response<UserResponse>
//
//    /**
//     * Refresh the authentication token
//     * @param request The token refresh request
//     * @return Response containing the new token
//     */
//    @POST("auth/refresh")
//    suspend fun refreshToken(@Body request: TokenRequest): Response<TokenResponse>
//
//    /**
//     * Send a password reset email
//     * @param request The email to send the reset link to
//     * @return Response indicating success or failure
//     */
//    @POST("auth/forgot-password")
//    suspend fun sendPasswordResetEmail(@Body request: Map<String, String>): Response<Unit>
//
//    /**
//     * Start phone number authentication by sending a verification code
//     * @param request The phone number to verify
//     * @return Response indicating success or failure
//     */
//    @POST("auth/phone/send-code")
//    suspend fun sendPhoneVerificationCode(@Body request: Map<String, String>): Response<Unit>
//
//    /**
//     * Verify the phone number with the received code
//     * @param request The verification request containing the code
//     * @return Response containing the authentication result
//     */
//    @POST("auth/phone/verify")
//    suspend fun verifyPhoneNumber(@Body request: Map<String, String>): Response<AuthResponse>
//}
//
///**
// * Response model for user profile
// */
//data class UserResponse(
//    val id: String,
//    val email: String? = null,
//    val phoneNumber: String? = null,
//    val displayName: String? = null,
//    val photoUrl: String? = null
//)
//
///**
// * Request model for token refresh
// */
//data class TokenRequest(
//    val refreshToken: String
//)
//
///**
// * Response model for token refresh
// */
//data class TokenResponse(
//    val token: String,
//    val expiresIn: Long
//)
