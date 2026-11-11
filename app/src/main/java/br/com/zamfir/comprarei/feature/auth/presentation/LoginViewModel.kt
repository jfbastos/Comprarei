package br.com.zamfir.comprarei.feature.auth.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidLogin
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository : UserRepository, private val firestoreRepository: FirestoreRepository, private val configRepository: ConfigRepository) : ViewModel() {

    private var _loginState = MutableLiveData<LoginState>()
    val loginState : LiveData<LoginState> get() = _loginState

    private var _userLoggedState = MutableLiveData<Boolean>()
    val userLoggedState : LiveData<Boolean> get() = _userLoggedState

    private var _userLoggedOffState = MutableLiveData<Boolean>()
    val userLoggedOffState : LiveData<Boolean> get() = _userLoggedOffState

    private var _localSaveState = MutableLiveData<Boolean>()
    val localSaveState : LiveData<Boolean> get() = _localSaveState

    private var _isLoginWithGoogle = MutableLiveData<Boolean>()
    val isLoginWithGoogle : LiveData<Boolean> get() = _isLoginWithGoogle


    fun login(email : String, password : String, isLoginWithGoogle : Boolean = false) = viewModelScope.launch {
        try{
            _loginState.value = LoginState(loading = true)

            val user = userRepository.loginUser(email, password, isLoginWithGoogle)
            configRepository.loggedWithGoogle(isLoginWithGoogle)

            firestoreRepository.getUserDataFromFirebase(onProgress = {}){
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

    fun getUserInfo() = viewModelScope.launch {
        _isLoginWithGoogle.value = configRepository.getIsUserLoggedFromGoogle()
    }

    fun hasLoggedUser() = viewModelScope.launch {
        _userLoggedState.value = userRepository.hasUserLogged()
    }

    fun logOff() = viewModelScope.launch {
        _userLoggedOffState.value = userRepository.logOffUser()
    }
}