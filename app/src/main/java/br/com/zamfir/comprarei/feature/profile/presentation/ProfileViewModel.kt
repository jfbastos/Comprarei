package br.com.zamfir.comprarei.feature.profile.presentation

import br.com.zamfir.comprarei.core.util.log.AppLogger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidPassword
import br.com.zamfir.comprarei.core.domain.exceptions.UserProfilePictureException
import br.com.zamfir.comprarei.core.presentation.UiText
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRespository: UserRepository) : ViewModel() {

    private var _profileState : MutableLiveData<ProfileState> = MutableLiveData()
    val profileState : LiveData<ProfileState> get() = _profileState

    private var _successfullySave : MutableLiveData<ProfileEditState> = MutableLiveData()
    val successfullySave : LiveData<ProfileEditState> get() = _successfullySave

    // Mensagem do diálogo de erro ao salvar; fica aqui para sobreviver à rotação.
    private val _saveError = MutableStateFlow<UiText?>(null)
    val saveError : StateFlow<UiText?> = _saveError.asStateFlow()

    private var _deleteUserException : MutableLiveData<Exception?> = MutableLiveData()
    val deleteUserException : LiveData<Exception?> get() = _deleteUserException

    fun getProfileInfos() = viewModelScope.launch {
        _profileState.value = ProfileState(userRespository.getUserName(), userRespository.getUserProfilePicture())
    }

    fun saveInfos(
        photo: ByteArray?,
        profileName: String,
        currentPassword: String,
        newPassword: String
    ) = viewModelScope.launch {
        try{
            userRespository.updateUser(profileName, currentPassword, newPassword, photo)
            _successfullySave.value = ProfileEditState(success = true)
        }catch (e : Exception){
            AppLogger.warning("Falha ao salvar o perfil: ${e.message}")
            _successfullySave.value = ProfileEditState(success = false)
            _saveError.value = e.toSaveErrorMessage()
        }
    }

    fun dismissSaveError() {
        _saveError.value = null
    }

    // Erros conhecidos já chegam com mensagem traduzida pelo repositório.
    private fun Exception.toSaveErrorMessage() : UiText {
        val knownMessage = message
        return when {
            (this is InvalidPassword || this is UserProfilePictureException) && !knownMessage.isNullOrBlank() -> UiText.DynamicString(knownMessage)
            this is FirebaseNetworkException -> UiText.StringResource(R.string.no_internet_connection)
            else -> UiText.StringResource(R.string.edit_profile_error)
        }
    }

    fun deleteUser() = viewModelScope.launch{
        try {
            userRespository.deleteUser()
            _deleteUserException.value = null
        }catch (e : Exception){
            AppLogger.error("Falha ao excluir o usuário", e)
            _deleteUserException.value = e
        }
    }
}