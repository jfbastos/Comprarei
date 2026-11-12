package br.com.zamfir.comprarei.feature.cart.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** Home: a lista de compras. */
@Serializable
data object CartsRoute : NavKey

fun EntryProviderScope<NavKey>.cartEntries(
    onOpenCart: (cartId: Int, cartName: String) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    entry<CartsRoute> {
        CartsRoot(
            viewModel = koinViewModel(),
            formViewModel = koinViewModel(),
            filterViewModel = koinViewModel(),
            onOpenCart = { cart -> onOpenCart(cart.id, cart.name) },
            onOpenCategories = onOpenCategories,
            onOpenSettings = onOpenSettings,
        )
    }
}
