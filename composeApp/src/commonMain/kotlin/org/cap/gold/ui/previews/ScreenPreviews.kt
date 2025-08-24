package org.cap.gold.ui.previews

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cap.gold.model.User

import org.cap.gold.ui.navigation.AppScreen
import org.cap.gold.ui.screens.OrdersScreen
import org.cap.gold.ui.screens.ProductsScreen
import org.cap.gold.ui.screens.ProfileScreen
import org.cap.gold.ui.screens.UsersScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

// Test user data for previews
private val testUser = User(
    id = "test_user_123",
    email = "test@example.com",
    name = "Test User",
    displayName = "Test User",
    phoneNumber = "1234567890",
    role = 0 // Admin role
)



@Preview
@Composable
fun OrdersScreenPreview() {
    MaterialTheme {
        Surface {
            OrdersScreen()
        }
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        Surface {
            ProfileScreen(
                user = testUser,
                onLogout = {}
            )
        }
    }
}

@Preview
@Composable
fun UsersScreenPreview() {
    MaterialTheme {
        Surface {
            UsersScreen()
        }
    }
}

@Preview
@Composable
fun MainNavigationPreview() {
    var currentRoute by remember { mutableStateOf(AppScreen.Products.route) }
    
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
                ) {
                    when (currentRoute) {
                        AppScreen.Products.route -> ProductsScreen(user = testUser)
                        AppScreen.Orders.route -> OrdersScreen()
                        AppScreen.Profile.route -> ProfileScreen(user = testUser, onLogout = {})
                        AppScreen.Users.route -> UsersScreen()
                    }
                }
                
                // Bottom Navigation
                NavigationBar {
                    listOf(
                        AppScreen.Products,
                        AppScreen.Orders,
                        AppScreen.Profile,
                        AppScreen.Users
                    ).forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = { currentRoute = screen.route }
                        )
                    }
                }
            }
        }
    }
}
