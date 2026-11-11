package br.com.zamfir.comprarei.feature.profile.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.image.ImageCompressor
import br.com.zamfir.comprarei.core.data.user.PasswordChange
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidPassword
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

data class EditProfileState(
    val isLoading: Boolean = true,
    val email: String = "",
    // Contas só Google não têm senha: a seção de senha fica oculta.
    val hasPasswordLogin: Boolean = false,
    val name: String = "",
    val initialName: String = "",
    val picture: Uri? = null,
    // Foto escolhida e ainda não salva.
    val newPicture: Uri? = null,
    val isPasswordSectionExpanded: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val nameError: UiText? = null,
    val isSaving: Boolean = false,
    val saveError: UiText? = null,
    val isDiscardDialogVisible: Boolean = false,
) {
    val displayedPicture: Uri? get() = newPicture ?: picture
    val passwordCheck: PasswordCheck get() = PasswordRules.check(newPassword)

    private val nameChanged: Boolean get() = name.trim() != initialName
    private val wantsPasswordChange: Boolean get() = currentPassword.isNotEmpty() || newPassword.isNotEmpty()

    val hasChanges: Boolean get() = nameChanged || newPicture != null || wantsPasswordChange

    val canSave: Boolean
        get() = hasChanges && !isSaving && !isLoading &&
            (!wantsPasswordChange || (currentPassword.isNotEmpty() && passwordCheck.isValid))

    fun nameToSave(): String? = name.trim().takeIf { nameChanged }
    fun passwordToSave(): PasswordChange? = if (wantsPasswordChange) PasswordChange(currentPassword, newPassword) else null
}

sealed interface EditProfileAction {
    data class OnLoad(val initialPicture: Uri?) : EditProfileAction
    data object OnBackClick : EditProfileAction
    data class OnNameChange(val value: String) : EditProfileAction
    data class OnPicturePicked(val uri: Uri) : EditProfileAction
    data object OnTogglePasswordSection : EditProfileAction
    data class OnCurrentPasswordChange(val value: String) : EditProfileAction
    data class OnNewPasswordChange(val value: String) : EditProfileAction
    data object OnSaveClick : EditProfileAction
    data object OnDismissSaveError : EditProfileAction
    data object OnConfirmDiscard : EditProfileAction
    data object OnCancelDiscard : EditProfileAction
    data object OnDeleteAccountClick : EditProfileAction
}

sealed interface EditProfileEvent {
    data object NavigateBack : EditProfileEvent
    data object Saved : EditProfileEvent
    data object NavigateToDeleteAccount : EditProfileEvent
}

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val eventChannel = Channel<EditProfileEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoaded = false

    fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.OnLoad -> load(action.initialPicture)
            EditProfileAction.OnBackClick -> {
                if (_state.value.hasChanges && !_state.value.isSaving) {
                    _state.update { it.copy(isDiscardDialogVisible = true) }
                } else {
                    sendEvent(EditProfileEvent.NavigateBack)
                }
            }
            is EditProfileAction.OnNameChange -> _state.update { it.copy(name = action.value, nameError = null) }
            is EditProfileAction.OnPicturePicked -> _state.update { it.copy(newPicture = action.uri) }
            EditProfileAction.OnTogglePasswordSection -> _state.update {
                it.copy(isPasswordSectionExpanded = !it.isPasswordSectionExpanded)
            }
            is EditProfileAction.OnCurrentPasswordChange -> _state.update { it.copy(currentPassword = action.value) }
            is EditProfileAction.OnNewPasswordChange -> _state.update { it.copy(newPassword = action.value) }
            EditProfileAction.OnSaveClick -> save()
            EditProfileAction.OnDismissSaveError -> _state.update { it.copy(saveError = null) }
            EditProfileAction.OnConfirmDiscard -> {
                _state.update { it.copy(isDiscardDialogVisible = false) }
                sendEvent(EditProfileEvent.NavigateBack)
            }
            EditProfileAction.OnCancelDiscard -> _state.update { it.copy(isDiscardDialogVisible = false) }
            EditProfileAction.OnDeleteAccountClick -> sendEvent(EditProfileEvent.NavigateToDeleteAccount)
        }
    }

    // Carrega uma vez por ViewModel: depois de girar a tela, o que foi digitado continua lá.
    private fun load(initialPicture: Uri?) {
        if (hasLoaded) return
        hasLoaded = true
        _state.update { it.copy(picture = initialPicture) }
        viewModelScope.launch {
            val name = userRepository.getUserName()
            val email = userRepository.getUserEmail()
            val hasPasswordLogin = userRepository.hasPasswordLogin()
            val picture = initialPicture ?: userRepository.getUserProfilePicture()
            _state.update {
                it.copy(
                    isLoading = false,
                    name = name,
                    initialName = name,
                    email = email,
                    hasPasswordLogin = hasPasswordLogin,
                    picture = picture,
                )
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (!current.canSave) return
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = UiText.StringResource(R.string.blank_name)) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val photo = current.newPicture?.let { imageCompressor.compress(it) }
                userRepository.updateUser(
                    userName = current.nameToSave(),
                    passwordChange = current.passwordToSave(),
                    photo = photo,
                )
                _state.update { it.copy(isSaving = false) }
                sendEvent(EditProfileEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.warning("Falha ao salvar o perfil: ${e.message}")
                _state.update { it.copy(isSaving = false, saveError = e.toSaveErrorMessage()) }
            }
        }
    }

    // Erros conhecidos já chegam com mensagem traduzida pelo repositório.
    private fun Exception.toSaveErrorMessage(): UiText {
        val knownMessage = message
        return when {
            (this is InvalidPassword || this is UserProfilePictureException) && !knownMessage.isNullOrBlank() -> UiText.DynamicString(knownMessage)
            this is FirebaseNetworkException -> UiText.StringResource(R.string.no_internet_connection)
            else -> UiText.StringResource(R.string.edit_profile_error)
        }
    }

    private fun sendEvent(event: EditProfileEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
