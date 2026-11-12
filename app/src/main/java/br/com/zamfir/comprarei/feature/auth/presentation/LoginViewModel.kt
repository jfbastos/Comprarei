package br.com.zamfir.comprarei.feature.auth.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.core.data.user.UserRepository
import kotlinx.coroutines.launch

/** Restou só o que a MainActivity usa (há usuário logado?). Some na unificação dos hosts (3.16). */
class LoginViewModel(private val userRepository : UserRepository) : ViewModel() {

    private var _userLoggedState = MutableLiveData<Boolean>()
    val userLoggedState : LiveData<Boolean> get() = _userLoggedState

    fun hasLoggedUser() = viewModelScope.launch {
        _userLoggedState.value = userRepository.hasUserLogged()
    }
}
