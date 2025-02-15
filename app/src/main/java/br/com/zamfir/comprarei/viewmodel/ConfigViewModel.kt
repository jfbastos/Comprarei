package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.data.repositories.ConfigRepository
import br.com.zamfir.comprarei.data.repositories.FirestoreRepository
import br.com.zamfir.comprarei.data.repositories.UserRepository
import br.com.zamfir.comprarei.viewmodel.states.ConfigState
import kotlinx.coroutines.launch

class ConfigViewModel(
    private val configRepository: ConfigRepository, private val firestoreRepository: FirestoreRepository, private val userRepository: UserRepository
) : ViewModel() {

    private var _configData : MutableLiveData<ConfigState> = MutableLiveData<ConfigState>()
    val configData : LiveData<ConfigState> get() = _configData

    fun toggleProductsToBottom(isEnabled : Boolean) = viewModelScope.launch{
        configRepository.toggleDoneItensToBottom(isEnabled)
    }

    fun toggleShowCartTotal(isEnabled: Boolean) = viewModelScope.launch {
        configRepository.toggleShowTotalCart(isEnabled)
    }

    fun doBackup() = viewModelScope.launch {
        firestoreRepository.doBackup()
    }

    fun getConfigData() = viewModelScope.launch {
        val lastTimeBackup = configRepository.getLastBackupTime()
        val isToMoveItensToBottom = configRepository.getIsToMoveToBottomDoneItens()
        val isToShowCartTotal = configRepository.getIsToShowTotalCart()
        val isAutoBackupOn = configRepository.getIsAutoBackupEnabled()
        val userName = userRepository.getUserName()

        _configData.value = ConfigState(
            dateLastBackup = lastTimeBackup,
            isToMoveItensToBottom = isToMoveItensToBottom,
            isToShowCartTotal = isToShowCartTotal,
            isAutoBackupOn = isAutoBackupOn,
            userName = userName,
            userProfilePicture = userRepository.getUserProfilePicture()
        )
    }

    fun toggleAutoBackup(isEnabled: Boolean) {
        configRepository.toggleAutoBackup(isEnabled)
    }


}