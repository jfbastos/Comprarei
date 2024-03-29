package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.repositories.LoginRepository
import br.com.zamfir.comprarei.viewmodel.states.LoginState
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository : LoginRepository) : ViewModel() {

    private var _loginState = MutableLiveData<LoginState>()
    private val loginState : LiveData<LoginState> get() = _loginState


    fun loginWithEmail(email : String, password : String) = viewModelScope.launch {
        loginRepository.loginUser(email, password)
    }

}