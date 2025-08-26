package org.cap.gold

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cap.gold.auth.AuthService
import org.cap.gold.auth.TokenManager
import org.cap.gold.di.initKoin
import org.cap.gold.ui.screens.HomeScreen
import org.cap.gold.ui.screens.LoginScreen
import org.cap.gold.ui.theme.AppTheme
import org.koin.compose.koinInject
import cafe.adriel.voyager.core.screen.Screen
import org.cap.gold.ui.navigation.ProvideAppNavigator
import org.cap.gold.ui.screens.SplashScreen

@Composable
fun App() {
    // Get AuthService from Koin
    val authService: AuthService = koinInject()
    val coroutineScope = rememberCoroutineScope()
    
    // Track authentication state directly from service
    val authState by authService.authState.collectAsState(initial = authService.currentUser)
    val isLoading by authService.isLoading.collectAsState()
    
    // Only show splash during the initial auth check
    var initialCheckDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        authService.checkAuthState()
        initialCheckDone = true
    }
    
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Show a splash only during the very first auth check
            if (!initialCheckDone && isLoading) {
                SplashScreen(onTimeout = {})
            } else if (authState != null) {
                // Show main app content when authenticated inside a Voyager Navigator
                cafe.adriel.voyager.navigator.Navigator(HomeRootScreen)
            } else {
                // Show login screen when not authenticated
                LoginScreen(
                    onLoginSuccess = { /* Navigation handled by authState */ }
                )
            }
        }
    }
}

// Stable root screen that does not capture non-serializable objects
object HomeRootScreen : Screen {
    @Composable
    override fun Content() {
        val authService: AuthService = koinInject()
        val coroutineScope = rememberCoroutineScope()
        val authState by authService.authState.collectAsState(initial = authService.currentUser)
        // If somehow authState is null here, avoid crashing
        val user = authState ?: return
        ProvideAppNavigator {
            HomeScreen(
                user = user,
                onLogout = {
                    coroutineScope.launch {
                        authService.signOut()
                    }
                }
            )
        }
    }
}