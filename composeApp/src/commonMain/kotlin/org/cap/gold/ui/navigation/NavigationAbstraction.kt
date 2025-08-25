package org.cap.gold.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.LocalNavigator
import org.cap.gold.model.User

// Sealed routes used across the app (library-agnostic)
sealed interface AppRoute {
    data object ManageCategories : AppRoute
    data class ProductDetail(val productId: String, val user: User) : AppRoute
    data class OrderDetail(val orderId: String) : AppRoute
}

// Navigator abstraction used by screens
interface AppNavigator {
    fun push(route: AppRoute)
    fun pop(): Boolean
}

// Voyager-backed implementation
private class VoyagerAppNavigator(
    private val navigator: Navigator
) : AppNavigator {
    override fun push(route: AppRoute) {
        navigator.push(route.toVoyagerScreen())
    }
    override fun pop(): Boolean = navigator.pop()
}

// Local provider for the navigator abstraction
val LocalAppNavigator = staticCompositionLocalOf<AppNavigator?> { null }

@Composable
fun ProvideAppNavigator(content: @Composable () -> Unit) {
    val voyagerNavigator = LocalNavigator.current
    if (voyagerNavigator != null) {
        CompositionLocalProvider(LocalAppNavigator provides VoyagerAppNavigator(voyagerNavigator)) {
            content()
        }
    } else {
        content()
    }
}

// Mapping from AppRoute to existing Voyager screen classes
private fun AppRoute.toVoyagerScreen(): Screen = when (this) {
    is AppRoute.ManageCategories -> ManageCategoriesVoyagerScreen()
    is AppRoute.ProductDetail -> ProductDetailVoyagerScreen(productId = productId, user = user)
    is AppRoute.OrderDetail -> OrderDetailVoyagerScreen(orderId = orderId)
}
