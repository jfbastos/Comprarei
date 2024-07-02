package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.repositories.ProfileRepository
import br.com.zamfir.comprarei.viewmodel.states.ProfileState
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    private var _profileState : MutableLiveData<ProfileState> = MutableLiveData()
    val profileState : LiveData<ProfileState> get() = _profileState

    private var _successfullySave : MutableLiveData<Boolean> = MutableLiveData()
    val successfullySave : LiveData<Boolean> get() = _successfullySave

    fun getProfileInfos(isPhotoCached : Boolean) = viewModelScope.launch {
        val userName = profileRepository.getUserName()
        if(!isPhotoCached){
            profileRepository.getUserProfilePicture {
                _profileState.value = ProfileState(userName, it)
            }
        }else{
            _profileState.value = ProfileState(userName, null)
        }
    }

    fun salvarInfos(
        photo: ByteArray?,
        profileName: String,
        currentPassword: String,
        newPassword: String
    ) = viewModelScope.launch {
        try{
            if(profileName.isNotBlank()) profileRepository.updateUser(profileName)
            if((currentPassword.isNotBlank() && newPassword.isNotBlank()) && currentPassword != newPassword) profileRepository.updatePassword(currentPassword, newPassword)
            if(photo != null)  profileRepository.updateProfilePicture(photo)
            _successfullySave.value = true
        }catch (e : Exception){
            e.printStackTrace()
            _successfullySave.value = false
        }

    }
}