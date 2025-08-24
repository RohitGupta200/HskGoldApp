package org.cap.gold.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import org.cap.gold.auth.AuthService
import org.cap.gold.auth.model.AuthResult
import org.cap.gold.model.User
import org.cap.gold.ui.components.BrandingImage
// Removed PhoneLoginForm; using inline fields for email/password and phone (on signup)
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit = {},
    authService: AuthService = koinInject()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSignUp by remember { mutableStateOf(false) }
    var signupName by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }
    // Use an independent scope so login work isn't cancelled when this composable leaves composition
    val coroutineScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    DisposableEffect(Unit) {
        onDispose { coroutineScope.cancel() }
    }

    fun validatePassword(pass: String): Boolean {
        return pass.length >= 6
    }

    fun handleAuth() {
        if (!email.contains("@") || !email.contains(".")) {
            errorMessage = "Please enter a valid email address"
            return
        }

        if (!validatePassword(password)) {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        if (isSignUp && signupName.isBlank()) {
            errorMessage = "Please enter your name"
            return
        }

        if (isSignUp && signupPhone.length < 10) {
            errorMessage = "Please enter a valid phone number (at least 10 digits)"
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val result = if (isSignUp) {
                    authService.createUserWithEmail(email, password, signupPhone, signupName)
                } else {
                    authService.signInWithEmail(email, password)
                }

                when (result) {
                    is AuthResult.Success -> {
                        // Navigation handled by App.kt observing authState
                    }
                    is AuthResult.Error -> errorMessage = result.message ?: "Authentication failed"
                    AuthResult.Loading -> {}
                }
            } catch (ce: CancellationException) {
                // Composition left; ignore and avoid touching state
                return@launch
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.message ?: "Please check your connection"}"
            } finally {
                if (isActive) {
                    isLoading = false
                }
            }
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandingImage(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 32.dp)
            )

            // Error message
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // App logo or title
            Text(
                text = if (isSignUp) "Create Your Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = if (isSignUp) "Sign up to get started" else "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                enabled = !isLoading,
                isError = errorMessage != null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                enabled = !isLoading,
                isError = errorMessage != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = "Toggle password visibility")
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Name (signup only)
            if (isSignUp) {
                OutlinedTextField(
                    value = signupName,
                    onValueChange = { signupName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Phone (signup only)
            if (isSignUp) {
                OutlinedTextField(
                    value = signupPhone,
                    onValueChange = { signupPhone = it },
                    label = { Text("Phone Number (required)") },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login/Sign up button
            Button(
                onClick = { handleAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && (!isSignUp || (signupPhone.isNotBlank() && signupName.isNotBlank()))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isSignUp) "Create Account" else "Sign In")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle between login and sign up
            TextButton(
                onClick = { isSignUp = !isSignUp },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isSignUp) "Already have an account? Sign in"
                    else "Don't have an account? Sign up",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onLoginSuccess = {},
            authService = object : AuthService {
                private val _authState = MutableStateFlow<User?>(null)

                override val currentUser: User? get() = _authState.value
                override val authState: StateFlow<User?> = _authState
                override val isLoading: StateFlow<Boolean>
                    get() = TODO("Not yet implemented")

                override suspend fun checkAuthState() {
                    TODO("Not yet implemented")
                }

                override val error: StateFlow<String?>
                    get() = TODO("Not yet implemented")

                override suspend fun signInWithEmail(email: String, password: String): AuthResult<User> {
                    val user = User(
                        id = "preview-user",
                        phoneNumber = "+10000000000",
                        email = email,
                        displayName = "Test User",
                        photoUrl = null
                    )
                    _authState.value = user
                    return AuthResult.Success(user)
                }

                override suspend fun createUserWithEmail(
                    email: String,
                    password: String,
                    phoneNumber: String,
                    displayName: String?
                ): AuthResult<User> {
                    val user = User(
                        id = "new-user",
                        phoneNumber = phoneNumber,
                        email = email,
                        displayName = displayName ?: "New User",
                        photoUrl = null
                    )
                    _authState.value = user
                    return AuthResult.Success(user)
                }

                override suspend fun signOut() {
                    _authState.value = null
                }

            }
        )
    }
}
