package br.com.zamfir.comprarei.feature.settings.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SettingsRoute : NavKey

/**
 * [onOpenProfile] recebe a foto já carregada (Uri em texto) para o Perfil mostrar na hora.
 * [onLoggedOut] é chamado depois do logoff, que já apagou os dados locais.
 */
fun EntryProviderScope<NavKey>.settingsEntries(
    onNavigateBack: () -> Unit,
    onOpenProfile: (profilePicture: String?) -> Unit,
    onLoggedOut: () -> Unit,
) {
    entry<SettingsRoute> {
        SettingsRoot(
            viewModel = koinViewModel(),
            onNavigateBack = onNavigateBack,
            onNavigateToProfile = { picture -> onOpenProfile(picture?.toString()) },
            onNavigateToLogin = onLoggedOut,
        )
    }
}
