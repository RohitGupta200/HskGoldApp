package org.cap.gold.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen

import org.cap.gold.model.User
import org.cap.gold.ui.screens.OrdersScreen
import org.cap.gold.ui.screens.ProductsScreen
import org.cap.gold.ui.screens.ProfileScreen
import org.cap.gold.ui.screens.UsersScreen

sealed class AppScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val createScreen: (User, () -> Unit) -> Screen
) {
    object Products : AppScreen(
        route = "products",
        title = "Products",
        icon = Icons.Default.ShoppingCart,
        createScreen = { user, _ ->
            object : Screen {
                @Composable
                override fun Content() {
                    ProductsScreen(user = user)
                }
            }
        }
    )
    
    object Orders : AppScreen(
        route = "orders",
        title = "Orders",
        icon = Icons.AutoMirrored.Filled.ListAlt,
        createScreen = { _, _ ->
            object : Screen {
                @Composable
                override fun Content() {
                    OrdersScreen()
                }
            }
        }
    )
    
    object Profile : AppScreen(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person,
        createScreen = { user, onLogout ->
            object : Screen {
                @Composable
                override fun Content() {
                    ProfileScreen(user = user, onLogout = onLogout)
                }
            }
        }
    )
    
    object Users : AppScreen(
        route = "users",
        title = "Users",
        icon = Icons.Default.People,
        createScreen = { user, _ ->
            object : Screen {
                @Composable
                override fun Content() {
                    if (user.role == 0) UsersScreen()
                }
            }
        }
    )
    
    companion object {
        val items = listOf(Products, Orders, Profile, Users)
    }
}
