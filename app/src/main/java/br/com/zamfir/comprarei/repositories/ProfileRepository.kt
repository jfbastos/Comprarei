package br.com.zamfir.comprarei.repositories

import android.net.Uri
import android.util.Log
import br.com.zamfir.comprarei.util.exceptions.InvalidPassword
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileRepository(private val dispatecher : CoroutineDispatcher) {

    private var auth : FirebaseAuth = Firebase.auth

    @Throws
    suspend fun getUserName() = withContext(dispatecher){
        return@withContext auth.currentUser?.displayName ?: ""
    }

    @Throws
    suspend fun getUserProfilePicture(callback : (Uri?) -> Unit) = withContext(dispatecher){
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
    suspend fun updateUser(userName : String) = withContext(dispatecher){
        val profileChange = userProfileChangeRequest {
            displayName = userName
        }

        auth.currentUser?.updateProfile(profileChange)?.await()
    }


    @Throws
    suspend fun updatePassword(oldPassword : String, newPassword : String) = withContext(dispatecher){
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
    suspend fun updateProfilePicture(data: ByteArray) = withContext(dispatecher){
        try{
            val storage = Firebase.storage
            val storageRef = storage.reference
            val imagesRef = storageRef.child("profilePictures")

            auth.currentUser?.let { user ->
                val userRef = imagesRef.child(user.uid)
                val imageRef = userRef.child("profilePicture.jpg")

                imageRef.putBytes(data).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d("DEBUG", "Foto salva com sucesso.")
                    } else {
                        Log.e("DEBUG", "Problema ao persistir a foto no storage ${it.exception}")
                    }
                }
            }
        }catch (e : Exception){
            Log.e("DEBUG", "Problema ao persistir a foto $e")
        }
    }




}