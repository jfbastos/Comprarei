package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.repositories.FirebaseRepository
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.exceptions.InvalidPassword
import br.com.zamfir.comprarei.util.exceptions.UserAlreadyExists
import br.com.zamfir.comprarei.viewmodel.states.LoginState
import kotlinx.coroutines.launch

class LoginViewModel(private val firebaseRepository : FirebaseRepository) : ViewModel() {

    private var _loginState = MutableLiveData<LoginState>()
    val loginState : LiveData<LoginState> get() = _loginState

    private var _userLoggedState = MutableLiveData<Boolean>()
    val userLoggedState : LiveData<Boolean> get() = _userLoggedState

    private var _userLoggedOffState = MutableLiveData<Boolean>()
    val userLoggedOffState : LiveData<Boolean> get() = _userLoggedOffState


    fun loginWithEmail(email : String, password : String) = viewModelScope.launch {
        try{
            _loginState.value = LoginState(loading = true)
            val user = firebaseRepository.loginUser(email, password)
            _loginState.value = LoginState(success = true, user = user)
        }catch (e : Exception){
            when (e) {
                is InvalidLogin -> _loginState.value = LoginState(success = false, error = e, msgError = e.msg)
                else -> _loginState.value = LoginState(success = false, error = e, msgError = null)
            }
        }
    }

    fun createUser(email : String, userName : String, password : String) = viewModelScope.launch{
        try{
            _loginState.value = LoginState(loading = true)
            val user = firebaseRepository.createUser(email,userName, password)
            _loginState.value = LoginState(success = true,user = user)
        }catch (e : Exception){
            when (e) {
                is UserAlreadyExists -> _loginState.value = LoginState(success = false, error = e, msgError = e.msg)
                is InvalidPassword -> _loginState.value = LoginState(success = false, error = e, msgError = e.msg)
                else -> _loginState.value = LoginState(success = false, error = e, msgError = null)
            }
        }
    }

    fun hasLoggedUser() = viewModelScope.launch {
        _userLoggedState.value = firebaseRepository.hasUserLogged()
    }

    fun logOff() = viewModelScope.launch {
        _userLoggedOffState.value = firebaseRepository.logOffUser()
    }

    fun forgotPassword(email : String) = viewModelScope.launch {
        firebaseRepository.forgotPassword(email)
    }
}