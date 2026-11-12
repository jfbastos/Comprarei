package br.com.zamfir.comprarei.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import br.com.zamfir.comprarei.feature.auth.presentation.SignInRoute
import br.com.zamfir.comprarei.feature.auth.presentation.authEntries
import br.com.zamfir.comprarei.feature.cart.presentation.CartsRoute
import br.com.zamfir.comprarei.feature.cart.presentation.cartEntries
import br.com.zamfir.comprarei.feature.category.presentation.CategoriesRoute
import br.com.zamfir.comprarei.feature.category.presentation.categoryEntries
import br.com.zamfir.comprarei.feature.product.presentation.ProductsRoute
import br.com.zamfir.comprarei.feature.product.presentation.productEntries
import br.com.zamfir.comprarei.feature.profile.presentation.ProfileRoute
import br.com.zamfir.comprarei.feature.profile.presentation.profileEntries
import br.com.zamfir.comprarei.feature.settings.presentation.SettingsRoute
import br.com.zamfir.comprarei.feature.settings.presentation.settingsEntries

/**
 * Navegação do app inteiro (Navigation 3). O back stack começa no login ou na Home;
 * entrar e sair da conta trocam o back stack todo, para o voltar não cruzar essa fronteira.
 */
@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    onLoggedIn: () -> Unit,
) {
    val backStack = rememberNavBackStack(if (isLoggedIn) CartsRoute else SignInRoute)
    val navigateBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = navigateBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            authEntries(
                backStack = backStack,
                onLoggedIn = {
                    onLoggedIn()
                    backStack.resetTo(CartsRoute)
                },
            )
            cartEntries(
                onOpenCart = { cartId, cartName -> backStack.add(ProductsRoute(cartId, cartName)) },
                onOpenCategories = { backStack.add(CategoriesRoute) },
                onOpenSettings = { backStack.add(SettingsRoute) },
            )
            productEntries(onNavigateBack = navigateBack)
            categoryEntries(onNavigateBack = navigateBack)
            settingsEntries(
                onNavigateBack = navigateBack,
                onOpenProfile = { picture -> backStack.add(ProfileRoute(picture)) },
                onLoggedOut = { backStack.resetTo(SignInRoute) },
            )
            profileEntries(
                backStack = backStack,
                onAccountDeleted = { backStack.resetTo(SignInRoute) },
            )
        },
    )
}

/** Troca o back stack inteiro por [route] sem deixá-lo vazio no meio do caminho. */
private fun NavBackStack<NavKey>.resetTo(route: NavKey) {
    add(route)
    while (size > 1) removeAt(0)
}
