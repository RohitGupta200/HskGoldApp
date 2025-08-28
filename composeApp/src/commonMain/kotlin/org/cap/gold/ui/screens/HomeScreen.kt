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
import org.cap.gold.di.userModule
import org.cap.gold.model.User
import org.cap.gold.ui.screens.admin.AdminOrdersScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    user: User,
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
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
                AccountScreen(user = user, onLogout = onLogout)
            }
        }
    }

    // Build bottom bar screens: always include Products first; include Users only for admins
    val rawScreens: List<AppScreen> = remember(user) {
        val others = AppScreen.items.filter { screen ->
            screen != AppScreen.Products && (screen != AppScreen.Users || user.role == 0)
        }
        val built = listOf(AppScreen.Products) + others
        if (built.isNotEmpty()) built else listOf(AppScreen.Products, AppScreen.Orders, AppScreen.Profile)
    }
    // Extra safety in case a transient null leaks in from recomposition
    val screens: List<AppScreen> = remember(rawScreens) { rawScreens.mapNotNull { it }.distinct() }
    // Resolve current screen from selected route with guard rails
    val currentScreen: AppScreen = remember(selectedRoute, screens) {
        screens.firstOrNull { it.route == selectedRoute } ?: screens.firstOrNull() ?: AppScreen.Products
    }

    // If the selected route is not available (e.g., role change removed a tab), realign selection
    LaunchedEffect(screens, selectedRoute) {
        val exists = screens.any { it.route == selectedRoute }
        val target = if (exists) selectedRoute else screens.firstOrNull()?.route ?: AppScreen.Products.route
        if (target != selectedRoute) {
            onSelectRoute(target)
        }
    }

    // Main content with bottom navigation
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            BottomNavigationBar(
                screens = screens,
                currentRoute = currentScreen.route,
                onNavigate = { route ->
                    onSelectRoute(route)
                }
            )
        }
    ) { innerPadding ->
        // Ensure child screens are padded by Scaffold's insets (status bar and bottom bar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Display the current screen based on the route
            when (val screen = currentScreen) {
                is AppScreen.Products -> ProductsScreen(user = user, navigator = navigator)
                is AppScreen.Orders ->  if (user.role == 0) AdminOrdersScreen() else OrdersScreen()
                is AppScreen.Users -> if (user.role == 0) UsersScreen() else {}
                is AppScreen.Profile -> screen.createScreen(user, onLogout).Content()

            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    screens: List<AppScreen>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        screens.forEach { screen ->
            // Defensive guard in case a transient null/invalid item appears during recomposition
            val route = try { screen.route } catch (_: Throwable) { return@forEach }
            val title = try { screen.title } catch (_: Throwable) { return@forEach }
            val icon = try { screen.icon } catch (_: Throwable) { return@forEach }
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = icon, 
                        contentDescription = title
                    ) 
                },
                label = { 
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall
                    ) 
                },
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
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
