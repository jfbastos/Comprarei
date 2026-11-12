package br.com.zamfir.comprarei.feature.profile.presentation.deleteaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.auth.GoogleSignInResult
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidLogin
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeoutException

data class DeleteAccountState(
    val isLoading: Boolean = true,
    val email: String = "",
    // Formas de confirmar a identidade, conforme os provedores da conta.
    val hasPasswordLogin: Boolean = false,
    val hasGoogleLogin: Boolean = false,
    val password: String = "",
    val passwordError: UiText? = null,
    val isCheckingPassword: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isReauthenticated: Boolean = false,
    val isConfirmDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val errorDialog: DeleteAccountError? = null,
) {
    val isBusy: Boolean get() = isCheckingPassword || isGoogleLoading || isDeleting
    val canCheckPassword: Boolean get() = password.isNotEmpty() && !isBusy
    val canDelete: Boolean get() = isReauthenticated && !isBusy
}

/** Erro mostrado em diálogo; o título diz se falhou a confirmação de identidade ou a exclusão. */
data class DeleteAccountError(val isDeletionError: Boolean, val message: UiText)

sealed interface DeleteAccountAction {
    data object OnBackClick : DeleteAccountAction
    data class OnPasswordChange(val value: String) : DeleteAccountAction
    data object OnConfirmPasswordClick : DeleteAccountAction
    data object OnGoogleClick : DeleteAccountAction
    data class OnGoogleResult(val result: GoogleSignInResult) : DeleteAccountAction
    data object OnDeleteClick : DeleteAccountAction
    data object OnConfirmDelete : DeleteAccountAction
    data object OnCancelDelete : DeleteAccountAction
    data object OnDismissError : DeleteAccountAction
}

sealed interface DeleteAccountEvent {
    data object NavigateBack : DeleteAccountEvent

    /** A tela pede a conta ao Credential Manager (precisa da Activity) e devolve OnGoogleResult. */
    data object RequestGoogleAccount : DeleteAccountEvent
    data object AccountDeleted : DeleteAccountEvent
}

class DeleteAccountViewModel(
    private val userRepository: UserRepository,
    private val firestoreRepository: FirestoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    private val eventChannel = Channel<DeleteAccountEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: DeleteAccountAction) {
        when (action) {
            DeleteAccountAction.OnBackClick -> if (!_state.value.isDeleting) sendEvent(DeleteAccountEvent.NavigateBack)
            is DeleteAccountAction.OnPasswordChange -> _state.update { it.copy(password = action.value, passwordError = null) }
            DeleteAccountAction.OnConfirmPasswordClick -> reauthenticateWithPassword()
            DeleteAccountAction.OnGoogleClick -> {
                if (_state.value.isBusy) return
                _state.update { it.copy(isGoogleLoading = true) }
                sendEvent(DeleteAccountEvent.RequestGoogleAccount)
            }
            is DeleteAccountAction.OnGoogleResult -> onGoogleResult(action.result)
            DeleteAccountAction.OnDeleteClick -> if (_state.value.canDelete) _state.update { it.copy(isConfirmDialogVisible = true) }
            DeleteAccountAction.OnConfirmDelete -> deleteAccount()
            DeleteAccountAction.OnCancelDelete -> _state.update { it.copy(isConfirmDialogVisible = false) }
            DeleteAccountAction.OnDismissError -> _state.update { it.copy(errorDialog = null) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val email = userRepository.getUserEmail()
            val hasPassword = userRepository.hasPasswordLogin()
            val hasGoogle = userRepository.hasGoogleLogin()
            _state.update {
                it.copy(
                    isLoading = false,
                    email = email,
                    // Sem provedor conhecido (não deveria acontecer), oferece a senha.
                    hasPasswordLogin = hasPassword || !hasGoogle,
                    hasGoogleLogin = hasGoogle,
                )
            }
        }
    }

    private fun reauthenticateWithPassword() {
        val current = _state.value
        if (!current.canCheckPassword) return

        _state.update { it.copy(isCheckingPassword = true) }
        viewModelScope.launch {
            try {
                userRepository.reauthenticateWithPassword(current.password)
                _state.update { it.copy(isCheckingPassword = false, isReauthenticated = true, password = "") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isCheckingPassword = false) }
                when (e) {
                    is InvalidLogin -> _state.update {
                        it.copy(passwordError = UiText.StringResource(R.string.delete_account_wrong_password))
                    }
                    else -> showReauthError(e)
                }
            }
        }
    }

    private fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            GoogleSignInResult.Cancelled -> _state.update { it.copy(isGoogleLoading = false) }
            GoogleSignInResult.NoAccount -> _state.update {
                it.copy(isGoogleLoading = false, errorDialog = reauthError(UiText.StringResource(R.string.google_no_account)))
            }
            is GoogleSignInResult.Failure -> _state.update {
                it.copy(isGoogleLoading = false, errorDialog = reauthError(UiText.StringResource(R.string.something_went_wrong_google_account)))
            }
            is GoogleSignInResult.Success -> viewModelScope.launch {
                try {
                    userRepository.reauthenticateWithGoogle(result.idToken)
                    _state.update { it.copy(isGoogleLoading = false, isReauthenticated = true) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(isGoogleLoading = false) }
                    when (e) {
                        // Escolheu uma conta Google diferente da logada no app.
                        is FirebaseAuthInvalidUserException -> _state.update {
                            it.copy(errorDialog = reauthError(UiText.StringResource(R.string.delete_account_google_mismatch)))
                        }
                        else -> showReauthError(e)
                    }
                }
            }
        }
    }

    private fun showReauthError(e: Exception) {
        val message = when (e) {
            is FirebaseNetworkException -> R.string.no_internet_connection
            is FirebaseTooManyRequestsException -> R.string.login_too_many_requests
            else -> {
                AppLogger.error("Falha ao reautenticar para excluir a conta", e)
                R.string.delete_account_reauth_failed
            }
        }
        _state.update { it.copy(errorDialog = reauthError(UiText.StringResource(message))) }
    }

    /**
     * Ordem: cancela backups → apaga a nuvem → apaga foto e conta → limpa o aparelho.
     * A conta sai por último porque, sem ela, não há mais permissão para apagar os dados do Firestore.
     * Em qualquer falha os dados locais ficam intactos (o próximo backup reenviaria o que já foi apagado).
     */
    private fun deleteAccount() {
        if (!_state.value.canDelete) return

        _state.update { it.copy(isConfirmDialogVisible = false, isDeleting = true) }
        viewModelScope.launch {
            try {
                firestoreRepository.cancelBackups()
                firestoreRepository.deleteUserData()
                userRepository.deleteUser()
                sendEvent(DeleteAccountEvent.AccountDeleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.error("Falha ao excluir a conta", e)
                _state.update { it.copy(isDeleting = false) }
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    // A confirmação de identidade expirou (o Firebase aceita só um login recente).
                    _state.update {
                        it.copy(
                            isReauthenticated = false,
                            errorDialog = reauthError(UiText.StringResource(R.string.delete_account_reauth_expired)),
                        )
                    }
                } else {
                    _state.update {
                        it.copy(errorDialog = DeleteAccountError(isDeletionError = true, message = e.toDeletionMessage()))
                    }
                }
            }
        }
    }

    private fun Exception.toDeletionMessage(): UiText {
        val isNetworkError = this is FirebaseNetworkException || this is TimeoutException ||
            (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE)
        return UiText.StringResource(if (isNetworkError) R.string.delete_account_network_error else R.string.delete_account_failed)
    }

    private fun reauthError(message: UiText) = DeleteAccountError(isDeletionError = false, message = message)

    private fun sendEvent(event: DeleteAccountEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
