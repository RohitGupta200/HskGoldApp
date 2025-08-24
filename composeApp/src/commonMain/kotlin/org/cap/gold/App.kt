package org.cap.gold

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cap.gold.auth.AuthService
import org.cap.gold.di.initKoin
import org.cap.gold.ui.screens.HomeScreen
import org.cap.gold.ui.screens.LoginScreen
import org.cap.gold.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    // Get AuthService from Koin
    val authService: AuthService = koinInject()
    val coroutineScope = rememberCoroutineScope()
    
    // Track authentication state
    var isAuthenticated by remember { mutableStateOf(authService.currentUser != null) }
    val authState by authService.authState.collectAsState(initial = authService.currentUser)
    val isLoading by authService.isLoading.collectAsState()
    
    // Check auth state when app starts
    LaunchedEffect(Unit) {
        authService.checkAuthState()
    }
    
    // Update UI when auth state changes
    LaunchedEffect(authState) {
        isAuthenticated = authState != null
    }
    
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Show a splash/loading screen while we determine auth state to avoid flashing Login
            if (isLoading) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else if (isAuthenticated) {
                // Show main app content when authenticated
                HomeScreen(
                    user = authState!!,
                    onLogout = {
                        coroutineScope.launch {
                            authService.signOut()
                            isAuthenticated = false
                        }
                    }
                )
            } else {
                // Show login screen when not authenticated
                LoginScreen(
                    onLoginSuccess = { isAuthenticated = true }
                )
            }
        }
    }
}