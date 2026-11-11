package br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordState(
    val isVisible: Boolean = false,
    val email: String = "",
    val emailError: UiText? = null,
    val isSending: Boolean = false,
    // E-mail para o qual o link foi pedido; quando preenchido, o sheet mostra a confirmação.
    val sentTo: String? = null,
)

sealed interface ForgotPasswordAction {
    data object OnOpen : ForgotPasswordAction
    data object OnDismiss : ForgotPasswordAction
    data class OnEmailChange(val email: String) : ForgotPasswordAction
    data object OnSendClick : ForgotPasswordAction
}

class ForgotPasswordViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            ForgotPasswordAction.OnOpen -> _state.value = ForgotPasswordState(isVisible = true)
            ForgotPasswordAction.OnDismiss -> _state.value = ForgotPasswordState()
            is ForgotPasswordAction.OnEmailChange -> _state.update { it.copy(email = action.email, emailError = null) }
            ForgotPasswordAction.OnSendClick -> sendResetLink()
        }
    }

    private fun sendResetLink() {
        if (_state.value.isSending) return

        val email = _state.value.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(emailError = UiText.StringResource(R.string.invalid_email)) }
            return
        }

        _state.update { it.copy(isSending = true, emailError = null) }
        viewModelScope.launch {
            try {
                userRepository.requestPasswordReset(email)
                _state.update { it.copy(isSending = false, sentTo = email) }
            } catch (e: FirebaseAuthInvalidUserException) {
                // A confirmação é neutra ("se existir uma conta..."), então e-mail sem conta também é sucesso.
                // Assim a tela não revela quais e-mails estão cadastrados.
                _state.update { it.copy(isSending = false, sentTo = email) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false, emailError = e.toErrorMessage()) }
            }
        }
    }

    private fun Exception.toErrorMessage(): UiText = when (this) {
        is FirebaseNetworkException -> UiText.StringResource(R.string.no_internet_connection)
        is FirebaseTooManyRequestsException -> UiText.StringResource(R.string.forgot_password_too_many_requests)
        is FirebaseAuthInvalidCredentialsException -> UiText.StringResource(R.string.invalid_email)
        else -> {
            AppLogger.error("Falha ao enviar o e-mail de redefinição de senha", this)
            UiText.StringResource(R.string.forgot_password_error)
        }
    }
}
