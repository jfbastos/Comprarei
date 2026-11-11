package br.com.zamfir.comprarei.feature.auth.presentation.createaccount

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.image.ImageCompressor
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidEmail
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidPassword
import br.com.zamfir.comprarei.core.domain.exceptions.UserAlreadyExists
import br.com.zamfir.comprarei.core.domain.exceptions.UserProfilePictureException
import br.com.zamfir.comprarei.core.domain.validation.PasswordCheck
import br.com.zamfir.comprarei.core.domain.validation.PasswordRules
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateAccountState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val picture: Uri? = null,
    val emailError: UiText? = null,
    val nameError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val isCreating: Boolean = false,
    // Erro que não pertence a um campo (rede, falha inesperada).
    val errorDialog: UiText? = null,
) {
    val passwordCheck: PasswordCheck get() = PasswordRules.check(password)
}

sealed interface CreateAccountAction {
    data class OnEmailChange(val value: String) : CreateAccountAction
    data class OnNameChange(val value: String) : CreateAccountAction
    data class OnPasswordChange(val value: String) : CreateAccountAction
    data class OnConfirmPasswordChange(val value: String) : CreateAccountAction
    data class OnPicturePicked(val uri: Uri) : CreateAccountAction
    data object OnCreateClick : CreateAccountAction
    data object OnDismissError : CreateAccountAction
    data object OnBackClick : CreateAccountAction
}

sealed interface CreateAccountEvent {
    /** Conta criada; [photoFailed] indica que só a foto não foi enviada. */
    data class AccountCreated(val photoFailed: Boolean) : CreateAccountEvent
    data object NavigateBack : CreateAccountEvent
}

class CreateAccountViewModel(
    private val userRepository: UserRepository,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateAccountState())
    val state: StateFlow<CreateAccountState> = _state.asStateFlow()

    private val eventChannel = Channel<CreateAccountEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: CreateAccountAction) {
        when (action) {
            is CreateAccountAction.OnEmailChange -> _state.update { it.copy(email = action.value, emailError = null) }
            is CreateAccountAction.OnNameChange -> _state.update { it.copy(name = action.value, nameError = null) }
            is CreateAccountAction.OnPasswordChange -> _state.update { it.copy(password = action.value, passwordError = null) }
            is CreateAccountAction.OnConfirmPasswordChange -> _state.update {
                it.copy(confirmPassword = action.value, confirmPasswordError = null)
            }
            is CreateAccountAction.OnPicturePicked -> _state.update { it.copy(picture = action.uri) }
            CreateAccountAction.OnCreateClick -> create()
            CreateAccountAction.OnDismissError -> _state.update { it.copy(errorDialog = null) }
            CreateAccountAction.OnBackClick -> if (!_state.value.isCreating) sendEvent(CreateAccountEvent.NavigateBack)
        }
    }

    private fun create() {
        val current = _state.value
        if (current.isCreating) return

        val email = current.email.trim()
        val name = current.name.trim()
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) UiText.StringResource(R.string.invalid_email) else null
        val nameError = if (name.isEmpty()) UiText.StringResource(R.string.invalid_name) else null
        val passwordError = if (!current.passwordCheck.isValid) UiText.StringResource(R.string.password_requisites) else null
        val confirmError = if (current.confirmPassword != current.password) UiText.StringResource(R.string.not_same_password) else null

        if (emailError != null || nameError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    nameError = nameError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        _state.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            try {
                val photo = current.picture?.let { uri ->
                    // Se a foto não puder ser lida, cria a conta sem ela em vez de bloquear o cadastro.
                    runCatching { imageCompressor.compress(uri) }
                        .onFailure { AppLogger.error("Falha ao ler a foto do cadastro", it) }
                        .getOrNull()
                }
                userRepository.createNewUserInFirebase(email, name, current.password, photo)
                sendEvent(CreateAccountEvent.AccountCreated(photoFailed = current.picture != null && photo == null))
            } catch (e: CancellationException) {
                throw e
            } catch (e: UserProfilePictureException) {
                sendEvent(CreateAccountEvent.AccountCreated(photoFailed = true))
            } catch (e: UserAlreadyExists) {
                _state.update { it.copy(isCreating = false, emailError = UiText.StringResource(R.string.user_already_exists)) }
            } catch (e: InvalidEmail) {
                _state.update { it.copy(isCreating = false, emailError = UiText.StringResource(R.string.invalid_email)) }
            } catch (e: InvalidPassword) {
                _state.update { it.copy(isCreating = false, passwordError = UiText.DynamicString(e.msg)) }
            } catch (e: FirebaseNetworkException) {
                _state.update { it.copy(isCreating = false, errorDialog = UiText.StringResource(R.string.no_internet_connection)) }
            } catch (e: Exception) {
                AppLogger.error("Falha ao criar a conta", e)
                _state.update { it.copy(isCreating = false, errorDialog = UiText.StringResource(R.string.create_account_failed)) }
            }
        }
    }

    private fun sendEvent(event: CreateAccountEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
