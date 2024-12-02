package br.com.zamfir.comprarei.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.data.repositories.UserRepository
import br.com.zamfir.comprarei.viewmodel.states.ProfileEditState
import br.com.zamfir.comprarei.viewmodel.states.ProfileState
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRespository: UserRepository) : ViewModel() {

    private var _profileState : MutableLiveData<ProfileState> = MutableLiveData()
    val profileState : LiveData<ProfileState> get() = _profileState

    private var _successfullySave : MutableLiveData<ProfileEditState> = MutableLiveData()
    val successfullySave : LiveData<ProfileEditState> get() = _successfullySave

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
            e.printStackTrace()
            _successfullySave.value = ProfileEditState(success = false, error = e)
        }
    }

    fun deleteUser() = viewModelScope.launch{
        try {
            userRespository.deleteUser()
            _deleteUserException.value = null
        }catch (e : Exception){
            Log.e("DEBUG", "Failed to delete user : ${e.stackTraceToString()}")
            _deleteUserException.value = e
        }
    }
}