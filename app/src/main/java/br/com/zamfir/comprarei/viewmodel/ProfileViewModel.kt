package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.repositories.UserRepository
import br.com.zamfir.comprarei.viewmodel.states.ProfileState
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRespository: UserRepository) : ViewModel() {

    private var _profileState : MutableLiveData<ProfileState> = MutableLiveData()
    val profileState : LiveData<ProfileState> get() = _profileState

    private var _successfullySave : MutableLiveData<Boolean> = MutableLiveData()
    val successfullySave : LiveData<Boolean> get() = _successfullySave

    fun getProfileInfos() = viewModelScope.launch {
        val userName = userRespository.getUserName()
        userRespository.getUserProfilePicture {
            _profileState.value = ProfileState(userName, it)
        }
    }

    fun salvarInfos(
        photo: ByteArray?,
        profileName: String,
        currentPassword: String,
        newPassword: String
    ) = viewModelScope.launch {
        try{
            if(profileName.isNotBlank()) userRespository.updateUser(profileName)
            if((currentPassword.isNotBlank() && newPassword.isNotBlank()) && currentPassword != newPassword) userRespository.updatePassword(currentPassword, newPassword)
            if(photo != null)  userRespository.updateProfilePicture(photo)
            _successfullySave.value = true
        }catch (e : Exception){
            e.printStackTrace()
            _successfullySave.value = false
        }

    }
}