package br.com.zamfir.comprarei.feature.auth.presentation

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.feature.auth.presentation.createaccount.CreateAccountRoot
import br.com.zamfir.comprarei.feature.auth.presentation.login.SignInRoot
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [resumeRestore]: já logado, mas a restauração dos dados não terminou; a tela retoma direto nela. */
@Serializable
data class SignInRoute(val resumeRestore: Boolean = false) : NavKey

@Serializable
data object CreateAccountRoute : NavKey

/** Login e cadastro. [onLoggedIn] leva para a parte logada do app (quem chama troca o back stack). */
fun EntryProviderScope<NavKey>.authEntries(
    backStack: NavBackStack<NavKey>,
    onLoggedIn: () -> Unit,
) {
    entry<SignInRoute> { route ->
        SignInRoot(
            viewModel = koinViewModel { parametersOf(route.resumeRestore) },
            forgotPasswordViewModel = koinViewModel(),
            onNavigateToMain = onLoggedIn,
            onNavigateToCreateAccount = { backStack.add(CreateAccountRoute) },
        )
    }
    entry<CreateAccountRoute> {
        val context = LocalContext.current
        CreateAccountRoot(
            viewModel = koinViewModel(),
            onNavigateBack = { backStack.removeLastOrNull() },
            onAccountCreated = { photoFailed ->
                if (photoFailed) Toast.makeText(context, R.string.account_created_without_photo, Toast.LENGTH_LONG).show()
                onLoggedIn()
            },
        )
    }
}
