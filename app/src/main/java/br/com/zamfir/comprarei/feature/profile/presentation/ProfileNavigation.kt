package br.com.zamfir.comprarei.feature.profile.presentation

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.feature.profile.presentation.deleteaccount.DeleteAccountRoot
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** [initialPicture]: Uri (em texto) da foto já carregada em Configurações. */
@Serializable
data class ProfileRoute(val initialPicture: String? = null) : NavKey

@Serializable
data object DeleteAccountRoute : NavKey

/** [onAccountDeleted] leva de volta ao login (quem chama troca o back stack). */
fun EntryProviderScope<NavKey>.profileEntries(
    backStack: NavBackStack<NavKey>,
    onAccountDeleted: () -> Unit,
) {
    entry<ProfileRoute> { route ->
        val context = LocalContext.current
        EditProfileRoot(
            viewModel = koinViewModel(),
            initialPicture = route.initialPicture?.toUri(),
            onNavigateBack = { backStack.removeLastOrNull() },
            onSaved = {
                Toast.makeText(context, R.string.profile_successfully_edited, Toast.LENGTH_SHORT).show()
                backStack.removeLastOrNull()
            },
            onNavigateToDeleteAccount = { backStack.add(DeleteAccountRoute) },
        )
    }
    entry<DeleteAccountRoute> {
        val context = LocalContext.current
        DeleteAccountRoot(
            viewModel = koinViewModel(),
            onNavigateBack = { backStack.removeLastOrNull() },
            onAccountDeleted = {
                Toast.makeText(context, R.string.account_deleted, Toast.LENGTH_LONG).show()
                onAccountDeleted()
            },
        )
    }
}
