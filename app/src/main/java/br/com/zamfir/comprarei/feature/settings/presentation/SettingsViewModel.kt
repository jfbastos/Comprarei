package br.com.zamfir.comprarei.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.firestore.BackupStatus
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.presentation.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val userName: String = "",
    val profilePicture: Uri? = null,
    val moveDoneToBottom: Boolean = false,
    val showCartTotal: Boolean = true,
    val autoBackup: Boolean = true,
    // Data formatada do último backup; vazia quando nunca houve backup.
    val lastBackup: String = "",
    val backupStatus: BackupStatus = BackupStatus.Idle,
    val dialog: SettingsDialog? = null,
    val isLoggingOff: Boolean = false,
)

enum class SettingsDialog { ConfirmBackup, ConfirmLogoff }

sealed interface SettingsAction {
    data object OnScreenShown : SettingsAction
    data object OnBackClick : SettingsAction
    data object OnEditProfileClick : SettingsAction
    data class OnMoveDoneToBottomChange(val enabled: Boolean) : SettingsAction
    data class OnShowCartTotalChange(val enabled: Boolean) : SettingsAction
    data class OnAutoBackupChange(val enabled: Boolean) : SettingsAction
    data object OnBackupClick : SettingsAction
    data object OnConfirmBackup : SettingsAction
    data object OnLogoffClick : SettingsAction
    data object OnConfirmLogoff : SettingsAction
    data object OnDismissDialog : SettingsAction
}

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data class NavigateToProfile(val profilePicture: Uri?) : SettingsEvent
    data object NavigateToLogin : SettingsEvent
    data class ShowMessage(val message: UiText) : SettingsEvent
}

class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val firestoreRepository: FirestoreRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val eventChannel = Channel<SettingsEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        observeManualBackup()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.OnScreenShown -> loadSettings()
            SettingsAction.OnBackClick -> sendEvent(SettingsEvent.NavigateBack)
            SettingsAction.OnEditProfileClick -> sendEvent(SettingsEvent.NavigateToProfile(_state.value.profilePicture))
            is SettingsAction.OnMoveDoneToBottomChange -> {
                _state.update { it.copy(moveDoneToBottom = action.enabled) }
                viewModelScope.launch { configRepository.toggleDoneItensToBottom(action.enabled) }
            }
            is SettingsAction.OnShowCartTotalChange -> {
                _state.update { it.copy(showCartTotal = action.enabled) }
                viewModelScope.launch { configRepository.toggleShowTotalCart(action.enabled) }
            }
            is SettingsAction.OnAutoBackupChange -> {
                _state.update { it.copy(autoBackup = action.enabled) }
                configRepository.toggleAutoBackup(action.enabled)
            }
            SettingsAction.OnBackupClick -> {
                if (!_state.value.backupStatus.isInProgress()) {
                    _state.update { it.copy(dialog = SettingsDialog.ConfirmBackup) }
                }
            }
            SettingsAction.OnConfirmBackup -> {
                _state.update { it.copy(dialog = null) }
                viewModelScope.launch { firestoreRepository.doBackup() }
            }
            SettingsAction.OnLogoffClick -> _state.update { it.copy(dialog = SettingsDialog.ConfirmLogoff) }
            SettingsAction.OnConfirmLogoff -> logoff()
            SettingsAction.OnDismissDialog -> _state.update { it.copy(dialog = null) }
        }
    }

    // Recarrega toda vez que a tela aparece, para refletir edições feitas no Perfil.
    private fun loadSettings() = viewModelScope.launch {
        val userName = userRepository.getUserName()
        val profilePicture = userRepository.getUserProfilePicture()
        _state.update {
            it.copy(
                userName = userName,
                profilePicture = profilePicture,
                moveDoneToBottom = configRepository.getIsToMoveToBottomDoneItens(),
                showCartTotal = configRepository.getIsToShowTotalCart(),
                autoBackup = configRepository.getIsAutoBackupEnabled(),
                lastBackup = configRepository.getLastBackupTime(),
            )
        }
    }

    private fun observeManualBackup() = viewModelScope.launch {
        var previous: BackupStatus? = null
        firestoreRepository.observeManualBackup().collect { status ->
            _state.update { it.copy(backupStatus = status) }
            // Só avisa quando o backup termina com a tela aberta; um resultado antigo não gera mensagem.
            if (previous?.isInProgress() == true) {
                when (status) {
                    BackupStatus.Succeeded -> {
                        _state.update { it.copy(lastBackup = configRepository.getLastBackupTime()) }
                        sendEvent(SettingsEvent.ShowMessage(UiText.StringResource(R.string.settings_backup_success)))
                    }
                    BackupStatus.Failed -> sendEvent(SettingsEvent.ShowMessage(UiText.StringResource(R.string.settings_backup_failed)))
                    else -> Unit
                }
            }
            previous = status
        }
    }

    private fun logoff() = viewModelScope.launch {
        _state.update { it.copy(isLoggingOff = true) }
        val success = userRepository.logOffUser()
        _state.update { it.copy(isLoggingOff = false, dialog = null) }
        if (success) {
            sendEvent(SettingsEvent.NavigateToLogin)
        } else {
            sendEvent(SettingsEvent.ShowMessage(UiText.StringResource(R.string.settings_logoff_failed)))
        }
    }

    private fun sendEvent(event: SettingsEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}

fun BackupStatus.isInProgress() = this == BackupStatus.WaitingForNetwork || this == BackupStatus.Running
