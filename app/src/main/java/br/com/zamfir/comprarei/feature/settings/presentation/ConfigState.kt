package br.com.zamfir.comprarei.feature.settings.presentation

import android.net.Uri

data class ConfigState(
    val dateLastBackup: String,
    val isToMoveItensToBottom: Boolean,
    val isToShowCartTotal: Boolean,
    val isAutoBackupOn : Boolean,
    val userName : String,
    val userProfilePicture : Uri?
)