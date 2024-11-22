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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

class UserRepository(private val context : Context, private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    private var auth : FirebaseAuth = Firebase.auth

    suspend fun hasUserLogged() = withContext(dispatcher){ auth.currentUser != null }

    suspend fun getUserFromDb() = withContext(dispatcher) { appDatabase.UserInfoDao().getUserInfo() }

    suspend fun requestPasswordReset(email : String) = withContext(dispatcher){ auth.sendPasswordResetEmail(email) }

    suspend fun loginUser(email : String, password : String) = withContext(dispatcher){
        try{
            val currentUser = auth.signInWithEmailAndPassword(email, password).await().user
            saveUserInfo()
            return@withContext currentUser
        }catch (e : Exception){
            when(e){
                is FirebaseAuthInvalidCredentialsException -> throw InvalidLogin(context.getString(R.string.msg_exp_user_password_invalid))
                else -> throw e
            }
        }
    }

    suspend fun createNewUserInFirebase(email: String, user: String, password: String, photoByte: ByteArray?) = withContext(dispatcher) {
        try {
            auth.createUserWithEmailAndPassword(email, password).await()

            updateUserName(user)

            if (photoByte != null) {
                try {
                    updateProfilePicture(photoByte)
                } catch (e: Exception) {
                    throw UserProfilePictureException(context.getString(R.string.msg_exp_upload_profile_picture_failed))
                }
            } else {
                saveUserInfo()
            }

            return@withContext auth.currentUser

        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthUserCollisionException -> throw UserAlreadyExists(context.getString(R.string.user_already_exists))
                is FirebaseAuthWeakPasswordException -> throw InvalidPassword(context.getString(R.string.password_not_meet_minimum_requirement))
                else -> throw e
            }
        }
    }

    suspend fun getUserName() : String = withContext(dispatcher){
        val firebaseUserName = auth.currentUser?.displayName ?: ""
        if(firebaseUserName.isNotBlank()) return@withContext firebaseUserName

        val currentUserName = appDatabase.UserInfoDao().getUserInfo()?.name
        if(!currentUserName.isNullOrBlank()) return@withContext currentUserName

        return@withContext ""
    }

    suspend fun getUserProfilePicture() = withContext(dispatcher){
        try{
            val userDb = getUserFromDb()

            if(userDb != null){
                val file = File(userDb.profilePicture)
                if(file.exists()) return@withContext file.toUri()
                else return@withContext null
            }

            return@withContext getProfilePictureUriFromFirestore()

        }catch (e : Exception){
            return@withContext null
        }
    }

    suspend fun saveUserInfo() = withContext(dispatcher){
        try{
            val currenUserInfo = getUserFromDb()

            if(currenUserInfo != null){
                val updatedUser = UserInfo(
                    name = getUserName(),
                    lastUpdate = LocalDateTime.now().toString(),
                    email = currenUserInfo.email,
                    profilePicture = currenUserInfo.profilePicture
                )

                appDatabase.UserInfoDao().save(updatedUser)
            }else{
                val newUser = UserInfo(
                    name = getUserName(),
                    lastUpdate = LocalDateTime.now().toString(),
                    email = auth.currentUser?.email ?: Constants.EMPTY_STRING,
                    profilePicture = getProfilePicturePath()
                )

                appDatabase.UserInfoDao().save(newUser)
            }
        }catch (e : Exception){
            Log.e("DEBUG", "Failed to persist user info image. Details : ${e.stackTraceToString()}")
        }
    }

    private suspend fun getProfilePicturePath() = withContext(dispatcher){
        if(getProfilePictureUriFromFirestore() != null){
            val localFile = File.createTempFile(Constants.PROFILE_PICTURE_DEFAULT_NAME, Constants.PROFILE_PICTURE_DEFAULT_EXTENSION)

            getImageRef().getFile(localFile).await()

            if(localFile.exists()) return@withContext localFile.absolutePath
            else Constants.EMPTY_STRING
        } else {
            return@withContext Constants.EMPTY_STRING
        }
    }

    suspend fun updateUser(userName: String, currentPassword: String, newPassword: String, photo: ByteArray?) = withContext(dispatcher){
        updateUserName(userName)

        updatePassword(currentPassword, newPassword)

        if (photo != null) {
            updateProfilePicture(photo)
        }else{
            saveUserInfo()
        }
    }

    private suspend fun updateUserName(userName: String) {
        val profileChange = userProfileChangeRequest {
            displayName = userName
        }

        auth.currentUser?.updateProfile(profileChange)?.await()
    }

    suspend fun updatePassword(oldPassword : String, newPassword : String) = withContext(dispatcher){
        try{
            if((oldPassword.isBlank() || newPassword.isBlank()) || oldPassword == newPassword) return@withContext
            val userEmail = auth.currentUser?.email ?: getUserFromDb()?.email ?: ""
            val authProvider = EmailAuthProvider.getCredential(userEmail, oldPassword)

            auth.currentUser?.reauthenticate(authProvider)?.await()
            auth.currentUser?.updatePassword(newPassword)?.await()
        }catch (e : Exception){
            if(e is FirebaseAuthInvalidCredentialsException) throw InvalidPassword(context.getString(R.string.your_current_password_is_incorrect))
            Log.e("DEBUG", "Fail to update password. Details : ${e.stackTraceToString()}")
        }

    }

    suspend fun updateProfilePicture(data: ByteArray) = withContext(dispatcher){
        try{
            appDatabase.UserInfoDao().getUserInfo()?.let {
                if (it.profilePicture.isNotBlank()) {
                    File(it.profilePicture).delete()
                }
            }

            val upload = getImageRef().putBytes(data).await()

            if(upload.task.isSuccessful) saveUserInfo()
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

    private fun runOnMainDispatcher(function: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch { function() }
    }

    private fun getImageRef(): StorageReference {
        val currentUser = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))
        val userRef = Firebase.storage.reference.child(Constants.FIRESTORE_STORAGE_NAME).child(currentUser.uid)

        return userRef.child(Constants.FIRESTORE_PROFILE_PICTURE_NAME)
    }

    private suspend fun getProfilePictureUriFromFirestore() : Uri?{
        return try{
            getImageRef().downloadUrl.await()
        }catch (e : Exception){
            null
        }

    }

}