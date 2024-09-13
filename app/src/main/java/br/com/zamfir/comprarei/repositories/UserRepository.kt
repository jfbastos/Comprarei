package br.com.zamfir.comprarei.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.model.AppDatabase
import br.com.zamfir.comprarei.model.entity.UserInfo
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.exceptions.InvalidPassword
import br.com.zamfir.comprarei.util.exceptions.NoUserLogged
import br.com.zamfir.comprarei.util.exceptions.UserAlreadyExists
import br.com.zamfir.comprarei.util.exceptions.UserInfoPersistenceException
import br.com.zamfir.comprarei.util.exceptions.UserProfilePictureException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

class UserRepository(private val context : Context, private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    private var auth : FirebaseAuth = Firebase.auth

    suspend fun hasUserLogged() = withContext(dispatcher){ auth.currentUser != null }

    suspend fun forgotPassword(email : String) = withContext(dispatcher){ auth.sendPasswordResetEmail(email) }

    @Throws
    private fun getImageRef(): StorageReference {
        val currentUser = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))
        val userRef = Firebase.storage.reference.child(Constants.FIRESTORE_STORAGE_NAME).child(currentUser.uid)

        return userRef.child(Constants.FIRESTORE_PROFILE_PICTURE_NAME)
    }

    @Throws
    suspend fun loginUser(email : String, password : String) = withContext(dispatcher){
        try{
            return@withContext auth.signInWithEmailAndPassword(email, password).await().user
        }catch (e : Exception){
            when(e){
                is FirebaseAuthInvalidCredentialsException -> throw InvalidLogin(context.getString(R.string.msg_exp_user_password_invalid))
                else -> throw e
            }
        }
    }

    @Throws
    suspend fun createUserInFirebase(email: String, user: String, password: String, photoByte: ByteArray?) = withContext(dispatcher){
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
            val profileChangeRequest = userProfileChangeRequest {
                displayName = user
            }

            if(photoByte != null){
                try{
                    getImageRef().putBytes(photoByte).await()
                }catch (e : Exception){
                    throw UserProfilePictureException(context.getString(R.string.msg_exp_upload_profile_picture_failed))
                }
            }

            auth.currentUser?.updateProfile(profileChangeRequest)?.await()

            return@withContext auth.currentUser

        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthUserCollisionException -> throw UserAlreadyExists(context.getString(R.string.user_already_exists))
                is FirebaseAuthWeakPasswordException -> throw InvalidPassword(context.getString(R.string.password_not_meet_minimum_requirement))
                else -> throw e
            }
      }
    }

    @Throws
    suspend fun getUserName() = withContext(dispatcher){
        return@withContext appDatabase.UserInfoDao().getUserInfo()?.name.takeIf { it.isNullOrBlank().not() } ?: auth.currentUser?.displayName ?: ""
    }

    @Throws
    suspend fun getUserProfilePicture(callback: (Uri?) -> Unit) = withContext(dispatcher){
        try{
            appDatabase.UserInfoDao().getUserInfo()?.let { userData ->
                File(userData.profilePicture).takeIf { it.exists() }?.let {
                    launch(Dispatchers.Main) { callback.invoke(it.toUri()) }
                    return@withContext
                }
            }

            val imageUri = getImageRef().downloadUrl.await()

            callback.invoke(imageUri)
        }catch (e : Exception){
            throw UserProfilePictureException(context.getString(R.string.failed_to_retrieve_profile_picture))
        }
    }

    @Throws
    suspend fun saveUserInfo(saveDone : () -> Unit) = withContext(dispatcher){
        val localFile = File.createTempFile(Constants.PROFILE_PICTURE_DEFAULT_NAME, Constants.PROFILE_PICTURE_DEFAULT_EXTENSION )

        val downloadUri = getImageRef().getFile(localFile).await()

        if(downloadUri.task.isSuccessful){
            persistInfos(localFile.absolutePath ?: Constants.EMPTY_STRING){ exception ->
                if(exception == null) launch(Dispatchers.Main) { saveDone.invoke() }
                else throw exception
            }
        }else{
            if (downloadUri.task.exception is StorageException) {
                val errorCode = (downloadUri.task.exception as StorageException).errorCode
                val innerException = (downloadUri.task.exception as StorageException)
                Log.e("DEBUG", "Failed to download image with error code $errorCode and exception being $innerException")
            }else{
                Log.e("DEBUG", "Failed to download image unknown error. Details : ${downloadUri.task.exception?.stackTraceToString()}")
            }
            saveDone.invoke()
            throw UserProfilePictureException(context.getString(R.string.failed_to_download_profile_picture))
        }
    }

    @Throws
    suspend fun updateUser(userName : String) = withContext(dispatcher){
        val profileChange = userProfileChangeRequest {
            displayName = userName
        }

        auth.currentUser?.updateProfile(profileChange)?.await()

        appDatabase.UserInfoDao().getUserInfo()?.let { userData ->
            val updatedUser = UserInfo(
                name = userName,
                lastUpdate = LocalDateTime.now().toString(),
                email = userData.email,
                profilePicture = userData.profilePicture
            ).apply {
                id = userData.id
            }

            appDatabase.UserInfoDao().save(updatedUser)
        }
    }

    @Throws
    suspend fun updatePassword(oldPassword : String, newPassword : String) = withContext(dispatcher){
        auth.currentUser?.email?.let {email ->
            val authProvider = EmailAuthProvider.getCredential(email, oldPassword)

            auth.currentUser?.reauthenticate(authProvider)?.addOnCompleteListener { task ->
                if(task.isComplete){
                    if(task.isSuccessful){
                        auth.currentUser?.updatePassword(newPassword)
                    }else{
                        throw InvalidPassword(context.getString(R.string.your_current_password_is_incorrect))
                    }
                }
            }
        }
    }

    @Throws
    suspend fun updateProfilePicture(data: ByteArray) = withContext(dispatcher){
        try{
            appDatabase.UserInfoDao().getUserInfo()?.let {
                if (it.profilePicture.isNotBlank()) {
                    File(it.profilePicture).delete()
                }
            }

            val upload = getImageRef().putBytes(data).await()

            if(upload.task.isSuccessful) saveUserInfo {}
            else throw UserProfilePictureException(context.getString(R.string.something_went_wrong_on_updating_profile_picture))
        }catch (e : Exception){
            Log.e("DEBUG", "Problem on profile picture persistence $e")
            UserProfilePictureException(context.getString(R.string.something_went_wrong_on_updating_profile_picture))
        }
    }

    suspend fun logOffUser() = withContext(dispatcher){
        return@withContext try{
            auth.signOut()
            appDatabase.UserInfoDao().getUserInfo()?.profilePicture?.let { File(it).delete() }
            appDatabase.clearAllTables()
            true
        }catch (e : Exception){
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUser() = withContext(dispatcher){
        val currentUser = auth.currentUser ?: throw RuntimeException(context.getString(R.string.no_user_logged))

        getImageRef().delete()
        currentUser.delete()
        auth.signOut()

        appDatabase.UserInfoDao().getUserInfo()?.let {
            if (it.profilePicture.isNotBlank()) {
                File(it.profilePicture).delete()
            }
        }

        appDatabase.clearAllTables()
    }

    @Throws
    private suspend fun persistInfos(photoPath : String, saveDone : (Exception?) -> Unit) = withContext(dispatcher){
        try {
            val idRegistroExistente = appDatabase.UserInfoDao().getUserInfo()?.id

            val user = UserInfo(
                name =  auth.currentUser?.displayName ?: Constants.EMPTY_STRING,
                email = auth.currentUser?.email ?: Constants.EMPTY_STRING,
                profilePicture = photoPath.takeIf { photoPath.isNotBlank() && File(photoPath).exists() } ?: Constants.EMPTY_STRING
            ).apply {
                if(idRegistroExistente != null) id = idRegistroExistente
            }

            appDatabase.UserInfoDao().save(user)
            saveDone.invoke(null)
        }catch (e : Exception){
            saveDone.invoke(UserInfoPersistenceException(e.message ?: context.getString(R.string.failed_to_save_user_but_exception_don_t_have_message)))
        }
    }
}