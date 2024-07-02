package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.repositories.FireAuthRepository
import br.com.zamfir.comprarei.repositories.FirestoreRepository
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.exceptions.InvalidPassword
import br.com.zamfir.comprarei.util.exceptions.UserAlreadyExists
import br.com.zamfir.comprarei.viewmodel.states.LoginState
import kotlinx.coroutines.launch

class LoginViewModel(private val fireAuthRepository : FireAuthRepository, private val firestoreRepository: FirestoreRepository) : ViewModel() {

    private var _loginState = MutableLiveData<LoginState>()
    val loginState : LiveData<LoginState> get() = _loginState

    private var _userLoggedState = MutableLiveData<Boolean>()
    val userLoggedState : LiveData<Boolean> get() = _userLoggedState

    private var _userLoggedOffState = MutableLiveData<Boolean>()
    val userLoggedOffState : LiveData<Boolean> get() = _userLoggedOffState


    fun loginWithEmail(email : String, password : String) = viewModelScope.launch {
        try{
            _loginState.value = LoginState(loading = true)
            val user = fireAuthRepository.loginUser(email, password)
            firestoreRepository.obterDadosDoUsuario{
                viewModelScope.launch {
                    _loginState.value = LoginState(success = true, user = user)
                }
            }

        }catch (e : Exception){
            when (e) {
                is InvalidLogin -> _loginState.value = LoginState(success = false, error = e, msgError = e.msg)
                else -> _loginState.value = LoginState(success = false, error = e, msgError = null)
            }
        }
    }

    fun createUser(email: String, userName: String, password: String, photoByte: ByteArray?) = viewModelScope.launch{
        try{
            _loginState.value = LoginState(loading = true)
            val user = fireAuthRepository.createUser(email,userName, password, photoByte)
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
        _userLoggedState.value = fireAuthRepository.hasUserLogged()
    }

    fun logOff() = viewModelScope.launch {
        _userLoggedOffState.value = fireAuthRepository.logOffUser()
    }

    fun forgotPassword(email : String) = viewModelScope.launch {
        fireAuthRepository.forgotPassword(email)
    }
}