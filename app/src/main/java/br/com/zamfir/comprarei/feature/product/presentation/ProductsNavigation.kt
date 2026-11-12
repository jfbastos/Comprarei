package br.com.zamfir.comprarei.feature.product.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Produtos de uma compra. O nome vai junto só para o título aparecer antes de carregar. */
@Serializable
data class ProductsRoute(val cartId: Int, val cartName: String) : NavKey

fun EntryProviderScope<NavKey>.productEntries(onNavigateBack: () -> Unit) {
    entry<ProductsRoute> { route ->
        ProductsRoot(
            viewModel = koinViewModel { parametersOf(route.cartId, route.cartName) },
            formViewModel = koinViewModel(),
            onNavigateBack = onNavigateBack,
        )
    }
}
