package br.com.zamfir.comprarei.feature.auth.presentation.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.firestore.RestoreStep
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidLogin
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import br.com.zamfir.comprarei.core.data.auth.GoogleSignInResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInState(
    val email: String = "",
    val password: String = "",
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val isSigningIn: Boolean = false,
    val isGoogleLoading: Boolean = false,
    // Etapa da restauração dos dados; não nula = tela de carregamento visível.
    val restoreStep: RestoreStep? = null,
    val errorDialog: UiText? = null,
    // Falha ao baixar os dados: oferece tentar de novo ou sair (nunca entra no app com dados incompletos).
    val isRestoreFailed: Boolean = false,
) {
    val isBusy: Boolean get() = isSigningIn || isGoogleLoading || restoreStep != null
}

sealed interface SignInAction {
    data class OnEmailChange(val value: String) : SignInAction
    data class OnPasswordChange(val value: String) : SignInAction
    data object OnSignInClick : SignInAction
    data object OnGoogleClick : SignInAction
    data class OnGoogleResult(val result: GoogleSignInResult) : SignInAction
    data object OnForgotPasswordClick : SignInAction
    data object OnCreateAccountClick : SignInAction
    data object OnDismissError : SignInAction
    data object OnRetryRestore : SignInAction
    data object OnGiveUpRestore : SignInAction
}

sealed interface SignInEvent {
    /** A tela pede a conta ao Credential Manager (precisa da Activity) e devolve OnGoogleResult. */
    data object RequestGoogleAccount : SignInEvent
    data object NavigateToMain : SignInEvent
    data object NavigateToCreateAccount : SignInEvent
    data object OpenForgotPassword : SignInEvent
}

class SignInViewModel(
    private val userRepository: UserRepository,
    private val firestoreRepository: FirestoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    private val eventChannel = Channel<SignInEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.OnEmailChange -> _state.update { it.copy(email = action.value, emailError = null) }
            is SignInAction.OnPasswordChange -> _state.update { it.copy(password = action.value, passwordError = null) }
            SignInAction.OnSignInClick -> signInWithEmail()
            SignInAction.OnGoogleClick -> {
                if (_state.value.isBusy) return
                _state.update { it.copy(isGoogleLoading = true) }
                sendEvent(SignInEvent.RequestGoogleAccount)
            }
            is SignInAction.OnGoogleResult -> onGoogleResult(action.result)
            SignInAction.OnForgotPasswordClick -> sendEvent(SignInEvent.OpenForgotPassword)
            SignInAction.OnCreateAccountClick -> if (!_state.value.isBusy) sendEvent(SignInEvent.NavigateToCreateAccount)
            SignInAction.OnDismissError -> _state.update { it.copy(errorDialog = null) }
            SignInAction.OnRetryRestore -> {
                _state.update { it.copy(isRestoreFailed = false) }
                restore()
            }
            SignInAction.OnGiveUpRestore -> giveUpRestore()
        }
    }

    private fun signInWithEmail() {
        val current = _state.value
        if (current.isBusy) return

        val email = current.email.trim()
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) UiText.StringResource(R.string.invalid_email) else null
        val passwordError = if (current.password.isEmpty()) UiText.StringResource(R.string.msg_warning_invalid_password) else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isSigningIn = true) }
        viewModelScope.launch {
            try {
                userRepository.loginUser(email, current.password)
                _state.update { it.copy(isSigningIn = false) }
                restore()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSigningIn = false) }
                handleSignInError(e)
            }
        }
    }

    private fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            GoogleSignInResult.Cancelled -> _state.update { it.copy(isGoogleLoading = false) }
            GoogleSignInResult.NoAccount -> _state.update {
                it.copy(isGoogleLoading = false, errorDialog = UiText.StringResource(R.string.google_no_account))
            }
            is GoogleSignInResult.Failure -> _state.update {
                it.copy(isGoogleLoading = false, errorDialog = UiText.StringResource(R.string.something_went_wrong_google_account))
            }
            is GoogleSignInResult.Success -> viewModelScope.launch {
                try {
                    userRepository.loginWithGoogle(result.idToken)
                    _state.update { it.copy(isGoogleLoading = false) }
                    restore()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(isGoogleLoading = false) }
                    handleSignInError(e)
                }
            }
        }
    }

    private fun handleSignInError(e: Exception) {
        when (e) {
            is InvalidLogin, is FirebaseAuthInvalidUserException -> _state.update {
                it.copy(passwordError = UiText.StringResource(R.string.wrong_user_or_password))
            }
            is FirebaseNetworkException -> _state.update { it.copy(errorDialog = UiText.StringResource(R.string.no_internet_connection)) }
            is FirebaseTooManyRequestsException -> _state.update { it.copy(errorDialog = UiText.StringResource(R.string.login_too_many_requests)) }
            else -> {
                AppLogger.error("Falha no login", e)
                _state.update { it.copy(errorDialog = UiText.StringResource(R.string.login_failed)) }
            }
        }
    }

    // Baixa os dados da nuvem antes de entrar no app.
    private fun restore() {
        _state.update { it.copy(restoreStep = RestoreStep.Preparing) }
        viewModelScope.launch {
            try {
                firestoreRepository.restoreUserData { step -> _state.update { it.copy(restoreStep = step) } }
                sendEvent(SignInEvent.NavigateToMain)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.error("Falha ao restaurar os dados após o login", e)
                _state.update { it.copy(restoreStep = null, isRestoreFailed = true) }
            }
        }
    }

    // Sai da conta e limpa o que foi baixado pela metade, para não ficar logado com dados incompletos.
    private fun giveUpRestore() {
        _state.update { it.copy(isRestoreFailed = false) }
        viewModelScope.launch { userRepository.logOffUser() }
    }

    private fun sendEvent(event: SignInEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
