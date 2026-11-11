package br.com.zamfir.comprarei.feature.profile.presentation

import br.com.zamfir.comprarei.core.util.log.AppLogger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.core.data.user.UserRepository
import kotlinx.coroutines.launch

/**
 * Restou só a exclusão de conta, usada pela DeleteAccountActivity.
 * A tela de perfil usa o EditProfileViewModel; este sai na migração da exclusão (3.13).
 */
class ProfileViewModel(private val userRespository: UserRepository) : ViewModel() {

    private var _deleteUserException : MutableLiveData<Exception?> = MutableLiveData()
    val deleteUserException : LiveData<Exception?> get() = _deleteUserException

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
