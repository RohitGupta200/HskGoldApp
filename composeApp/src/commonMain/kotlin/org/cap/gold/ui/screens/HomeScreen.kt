package org.cap.gold.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cap.gold.ui.navigation.AppScreen

// Navigation imports
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.cap.gold.model.User
import org.cap.gold.ui.screens.admin.AdminOrdersScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    user: User,
    onLogout: () -> Unit
) {
    val navigator = LocalNavigator.current
    
    // Check if we're in preview mode
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        // We'll handle preview mode in the Preview composable
        return@HomeScreen
    }

    // Create a custom profile screen with user and logout handler
    val profileScreen = remember(user, onLogout) {
        object : Screen {
            @Composable
            override fun Content() {
                ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }

    // Build bottom bar screens: always include Products first; include Users only for admins
    val screens = remember(user) {
        val others = AppScreen.items.filter { screen ->
            screen != AppScreen.Products && (screen != AppScreen.Users || user.role == 0)
        }
        val built = listOf(AppScreen.Products) + others
        if (built.isNotEmpty()) built else listOf(AppScreen.Products, AppScreen.Orders, AppScreen.Profile)
    }

    val currentScreen = remember(screens) { mutableStateOf<AppScreen?>(screens.firstOrNull()) }

    // Main content with bottom navigation
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                screens = screens,
                currentRoute = currentScreen.value?.route ?: screens.first().route,
                onNavigate = { route ->
                    // Defensive lookup that never dereferences null
                    val target = try {
                        screens.firstOrNull { it.route == route } ?: screens.firstOrNull()
                    } catch (_: Exception) {
                        null
                    }
                    val screen = target ?: AppScreen.Products
                    if (screen.route != (currentScreen.value?.route ?: "")) {
                        currentScreen.value = screen
                        navigator?.push(screen.createScreen(user, onLogout))
                    }
                }
            )
        }
    ) { innerPadding ->
        // Display the current screen based on the route
        when (val screen = currentScreen.value ?: AppScreen.Products) {
            is AppScreen.Products -> ProductsScreen(user = user, navigator = navigator)
            is AppScreen.Orders ->  if (user.role == 0) AdminOrdersScreen() else OrdersScreen()
            is AppScreen.Profile -> screen.createScreen(user, onLogout).Content()
            is AppScreen.Users -> if (user.role == 0) UsersScreen() else {}
        }
    }
}

@Composable
private fun BottomNavigationBar(
    screens: List<AppScreen>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    
    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = screen.icon, 
                        contentDescription = screen.title
                    ) 
                },
                label = { 
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall
                    ) 
                },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}

@Composable
fun ProfileScreen(
    user: User,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // User profile
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome message
        Text(
            text = "Welcome, ${user.displayName ?: user.phoneNumber}",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        // Email intentionally hidden; phone number is the visible primary identifier
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Logout button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Logout")
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }


}

@Composable
private fun PreviewHomeScreen(user: User) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Home Screen - Preview Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("User: ${user.name ?: "Unknown"}")
            Text("Email: ${user.email ?: "No email"}")
            Text("Role: ${if (user.role == 0) "Admin" else "User"}")
        }
    }
}

@Composable
@Preview
private fun BottomNavigationPreview() {
    // Simple preview state
    var currentRoute by remember { mutableStateOf(AppScreen.Products.route) }
    
    // Preview container with theme
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Current Screen: ${currentRoute.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Bottom Navigation Bar
                BottomNavigationBar(
                    screens = AppScreen.items,
                    currentRoute = currentRoute,
                    onNavigate = { route -> 
                        currentRoute = route 
                    }
                )
            }
        }
    }
}
