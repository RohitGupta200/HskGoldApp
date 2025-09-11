package org.cap.gold.ui.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cap.gold.model.User
import org.cap.gold.ui.screens.OrdersScreen
import org.cap.gold.ui.screens.admin.ManageCategoriesScreen
import org.cap.gold.ui.screens.order.OrderDetailScreen
import org.cap.gold.ui.screens.order.OrderDetailViewModel
import org.cap.gold.ui.screens.product.ProductDetailScreen
import org.cap.gold.ui.screens.product.ProductDetailViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class ManageCategoriesVoyagerScreen : Screen {
    override val key: ScreenKey = "manage_categories"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        ManageCategoriesScreen(
            onBack = { navigator?.pop() }
        )
    }
}

class ProductDetailVoyagerScreen(
    private val productId: String,
    private val user: User
) : Screen {
    override val key: ScreenKey = "product_detail_$productId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel: ProductDetailViewModel = koinInject(
            parameters = { parametersOf(productId, user.role == 0, user.role == 1) }
        )
        ProductDetailScreen(
            viewModel = viewModel,
            user = user,
            onBackClick = { navigator?.pop() },
            onOrderSuccess = { navigator?.pop()
                navigator?.push(object : Screen {
                    override val key: ScreenKey = "orders_from_product_update"
                    @Composable
                    override fun Content() {
                        OrdersScreen(User = user)
                    }
                })},
            onProductUpdated = { navigator?.pop() },
            onProductDeleted = { navigator?.pop() }
        )
    }
}

class OrderDetailVoyagerScreen(
    private val orderId: String
) : Screen {
    override val key: ScreenKey = "order_detail_$orderId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel: OrderDetailViewModel = koinInject()
        OrderDetailScreen(
            viewModel = viewModel,
            orderId = orderId,
            onBack = { navigator?.pop() }
        )
    }
}
