package org.cap.gold.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.cap.gold.auth.model.AuthResult
import org.cap.gold.model.User

/**
 * Interface defining the authentication service contract.
 * Handles user authentication, session management, and user profile operations.
 */
interface AuthService {
    // Email and Password Authentication

    /**
     * Signs in a user with their email and password.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult<User>

    /**
     * Creates a new user account with email, password, and mandatory phone number.
     */
    suspend fun createUserWithEmail(email: String, password: String, phoneNumber: String, displayName: String? = "new"): AuthResult<User>
    
    // Common
    
    /**
     * Signs out the current user.
     */
    suspend fun signOut()
    
    /**
     * The currently authenticated user, or null if no user is signed in.
     */
    val currentUser: User?
    
    /**
     * A flow that emits the current authentication state.
     * Emits null when no user is authenticated, or the User object when a user is authenticated.
     */
    val authState: Flow<User?>
    
    /**
     * A state flow indicating whether an authentication operation is in progress.
     */
    val isLoading: StateFlow<Boolean>
    
    /**
     * Checks the current authentication state by validating the stored tokens.
     * Updates the auth state based on token validity.
     */
    suspend fun checkAuthState()
    
    /**
     * A state flow containing the last error message, if any.
     * Cleared when a new operation starts.
     */
    val error: StateFlow<String?>
    
    // Account Recovery (disabled per requirements) – intentionally omitted
    
    // User Management – handled elsewhere; no methods here currently
}
