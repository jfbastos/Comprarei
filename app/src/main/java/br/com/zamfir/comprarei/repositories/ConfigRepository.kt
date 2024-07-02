package br.com.zamfir.comprarei.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.util.DateUtil
import br.com.zamfir.comprarei.worker.BackupWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ConfigRepository(private val context : Context,private val dispatcher: CoroutineDispatcher) {

    private val sharedPref = context.getSharedPreferences(context.getString(R.string.shared_file_name), Context.MODE_PRIVATE)

    private val auth = Firebase.auth

    suspend fun toggleDoneItensToBottom(isEnable : Boolean) = withContext(dispatcher) {
        with (sharedPref.edit()) {
            putBoolean(context.getString(R.string.donetobottom_key), isEnable)
            apply()
        }
    }

    suspend fun toggleShowTotalCart(isEnable: Boolean) = withContext(dispatcher){
        with(sharedPref.edit()){
            putBoolean(context.getString(R.string.showtotalcart_key), isEnable)
            apply()
        }
    }

    suspend fun doBackup() = withContext(dispatcher){
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED))
                .build()

        WorkManager.getInstance(context).enqueue(uploadWorkRequest)
    }

    suspend fun getLastBackupTime() = withContext(dispatcher){
        return@withContext DateUtil.formatDate(sharedPref.getString(context.getString(R.string.last_backup_key), "") ?: "")
    }

    suspend fun getIsToMoveToBottomDoneItens() = withContext(dispatcher){
        return@withContext sharedPref.getBoolean(context.getString(R.string.donetobottom_key), false)
    }

    suspend fun getIsToShowTotalCart() =  withContext(dispatcher){
        return@withContext sharedPref.getBoolean(context.getString(R.string.showtotalcart_key), true)
    }

    suspend fun getUserName() = withContext(dispatcher){
        return@withContext auth.currentUser?.displayName ?: ""
    }

    suspend fun getUserProfilePictureUrl(callback : (Uri?) -> Unit) = withContext(dispatcher){
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imagesRef = storageRef.child("profilePictures")

        auth.currentUser?.let { user ->
            val userRef = imagesRef.child(user.uid)
            val imageRef = userRef.child("profilePicture.jpg")

            imageRef.downloadUrl.addOnSuccessListener {
                callback.invoke(it)
            }.addOnFailureListener {
                Log.e("DEBUG", "Failed to load profile picture : $it")
                callback.invoke(null)
            }
        }
    }

}