package br.com.zamfir.comprarei.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.repositories.FirebaseRepository
import br.com.zamfir.comprarei.viewmodel.states.LoginState
import com.google.firebase.auth.FirebaseUser
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
            val user = firebaseRepository.loginUser(email, password)
            Log.d("DEBUG", "User : ${(user as FirebaseUser).uid} | ${(user as FirebaseUser).displayName}")
        }catch (e : Exception){
            Log.d("DEBUG", "User : $e")
        }
    }

    fun createUser(email : String, userName : String, password : String) = viewModelScope.launch{
        try{
            val user = firebaseRepository.createUser(email,userName, password)
            Log.d("DEBUG", "User : ${user?.uid} | ${user?.displayName}")
        }catch (e : Exception){
            Log.d("DEBUG", "User : $e")
        }
    }

    fun hasLoggedUser() = viewModelScope.launch {
        _userLoggedState.value = firebaseRepository.hasUserLogged()
    }

    fun logOff() = viewModelScope.launch {
        _userLoggedOffState.value = firebaseRepository.logOffUser()
    }
}