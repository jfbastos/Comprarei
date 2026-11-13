package br.com.zamfir.comprarei.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.preferences.ThemeMode
import br.com.zamfir.comprarei.core.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Primeira tela ao abrir o app. */
enum class StartDestination {
    SignIn,

    /** Logado, mas a restauração dos dados não terminou: volta ao login para concluí-la. */
    Restore,
    Home,
}

class MainViewModel(
    userRepository: UserRepository,
    firestoreRepository: FirestoreRepository,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    // Nulo enquanto verifica; a splash screen fica na tela até haver resposta.
    private val _startDestination = MutableStateFlow<StartDestination?>(null)
    val startDestination: StateFlow<StartDestination?> = _startDestination.asStateFlow()

    // Valor inicial lido na hora, para a primeira tela já abrir no tema certo.
    val themeMode: StateFlow<ThemeMode> = configRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, configRepository.getThemeMode())

    fun shouldAskNotificationPermission(): Boolean = !configRepository.wasNotificationPermissionAsked()

    fun onNotificationPermissionAsked() = configRepository.setNotificationPermissionAsked()

    init {
        viewModelScope.launch {
            _startDestination.value = when {
                !userRepository.hasUserLogged() -> StartDestination.SignIn
                firestoreRepository.needsRestore() -> StartDestination.Restore
                else -> StartDestination.Home
            }
        }
    }
}
