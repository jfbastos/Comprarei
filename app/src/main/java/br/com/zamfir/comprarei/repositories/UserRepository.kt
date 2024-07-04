package br.com.zamfir.comprarei.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import br.com.zamfir.comprarei.model.AppDatabase
import br.com.zamfir.comprarei.model.entity.UserInfo
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.exceptions.InvalidPassword
import br.com.zamfir.comprarei.util.exceptions.UserAlreadyExists
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
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

    suspend fun hasUserLogged() = withContext(dispatcher){
        return@withContext auth.currentUser != null
    }

    suspend fun forgotPassword(email : String) = withContext(dispatcher){
        auth.sendPasswordResetEmail(email)
    }

    @Throws
    suspend fun loginUser(email : String, password : String) = withContext(dispatcher){
        try{
            return@withContext auth.signInWithEmailAndPassword(email, password).await().user
        }catch (e : Exception){
            when(e){
                is FirebaseAuthInvalidCredentialsException -> throw InvalidLogin("Usuário ou senha inválidos.")
                else -> throw e
            }
        }
    }

    @Throws
    suspend fun createUser(email: String, user: String, password: String, photoByte: ByteArray?) = withContext(dispatcher){
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
            val profileChangeRequest = userProfileChangeRequest {
                displayName = user
            }

            if(photoByte != null){
                try{
                    val storage = Firebase.storage
                    val storageRef = storage.reference
                    val imagesRef = storageRef.child("profilePictures")

                    auth.currentUser?.let { user ->
                        val userRef = imagesRef.child(user.uid)
                        val imageRef = userRef.child("profilePicture.jpg")

                        imageRef.putBytes(photoByte).addOnFailureListener { task ->
                            Log.e("ERROR", "Failed to upload profile picture ${task.cause}")
                        }
                    }
                }catch (e : Exception){
                    Log.e("ERROR", "Problema ao persistir a foto $e")
                }
            }

            auth.currentUser?.updateProfile(profileChangeRequest)?.await()

            return@withContext auth.currentUser

        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthUserCollisionException -> throw UserAlreadyExists("Já existe um usuário com o e-mail informado.")
                is FirebaseAuthWeakPasswordException -> throw InvalidPassword("Senha fornecida não está de acordo com os critérios mínimos.")
                else -> throw e
            }
      }
    }

    @Throws
    suspend fun getUserName() = withContext(dispatcher){
        return@withContext appDatabase.UserInfoDao().getUserInfo()?.name.takeIf { it.isNullOrBlank().not() } ?: auth.currentUser?.displayName ?: ""
    }

    @Throws
    suspend fun getUserProfilePictureFromFireStorage(callback : (Uri?) -> Unit) = withContext(dispatcher){
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

    @Throws
    suspend fun getUserProfilePicture(callback: (Uri?) -> Unit) = withContext(dispatcher){
        appDatabase.UserInfoDao().getUserInfo()?.let { userData ->
            File(userData.profilePicture).takeIf { it.exists() }?.let {
                launch(Dispatchers.Main) { callback.invoke(it.toUri()) }
                return@withContext
            }
        }

        getUserProfilePictureFromFireStorage {
            callback.invoke(it)
        }
    }

    @Throws
    suspend fun saveUserInfo() = withContext(dispatcher){
        try{
            val storage = Firebase.storage
            val storageRef = storage.reference
            val imagesRef = storageRef.child("profilePictures")

            auth.currentUser?.let { user ->
                val localFile =  File.createTempFile("profilePhoto_", ".jpg")

                val userRef = imagesRef.child(user.uid)
                val imageRef = userRef.child("profilePicture.jpg")

                val download = imageRef.getFile(localFile).await()

                if(download.task.isSuccessful){
                    persistInfos(localFile.absolutePath ?: "")
                }else{
                    if (download.task.exception is StorageException) {
                        val errorCode = (download.task.exception as StorageException).errorCode
                        val innerException = (download.task.exception as StorageException)
                        Log.e("DEBUG", "Failed to download image with error code $errorCode and exception being $innerException")
                    }else{
                        Log.e("DEBUG", "Unknown exceptuion : ${download.task.exception}")
                    }
                }
            }
        }catch (e : Exception){
            Log.d("DEBUG", "Exception on download image : ${e.stackTraceToString()}")
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
                        throw InvalidPassword("Senha atual incorreta")
                    }
                }
            }
        }
    }

    @Throws
    suspend fun updateProfilePicture(data: ByteArray) = withContext(dispatcher){
        try{
            val storage = Firebase.storage
            val storageRef = storage.reference
            val imagesRef = storageRef.child("profilePictures")

            auth.currentUser?.let { user ->
                val userRef = imagesRef.child(user.uid)
                val imageRef = userRef.child("profilePicture.jpg")

                val upload = imageRef.putBytes(data).await()

                if(upload.task.isSuccessful) saveUserInfo()
                else Log.e("ERROR", "Problema ao persistir a foto ${upload.task.exception}")
            }
        }catch (e : Exception){
            Log.e("DEBUG", "Problema ao persistir a foto $e")
        }
    }

    suspend fun logOffUser() = withContext(dispatcher){
        return@withContext try{
            auth.signOut()
            appDatabase.clearAllTables()
            true
        }catch (e : Exception){
            e.printStackTrace()
            false
        }
    }

    private suspend fun persistInfos(photoPath : String) = withContext(dispatcher){
        if(photoPath.isNotBlank() && File(photoPath).exists()){
            val userName = auth.currentUser?.displayName
            val userEmail = auth.currentUser?.email
            appDatabase.UserInfoDao().save(UserInfo(userName ?: "", userEmail ?: "", photoPath))
            Log.d("DEBUG", "Updating profile picture path...")
        }else{
            Log.d("DEBUG", "Photo doent existis...")
        }
    }
}