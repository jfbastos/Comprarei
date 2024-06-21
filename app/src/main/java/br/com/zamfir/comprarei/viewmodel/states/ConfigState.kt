package br.com.zamfir.comprarei.viewmodel.states

import android.net.Uri

data class ConfigState(
    val dateLastBackup: String,
    val isToMoveItensToBottom: Boolean,
    val isToShowCartTotal: Boolean,
    val userName : String,
    val userProfilePicture : Uri?
)