package br.com.zamfir.comprarei.repositories

import android.util.Log
import br.com.zamfir.comprarei.model.AppDatabase
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.exceptions.InvalidPassword
import br.com.zamfir.comprarei.util.exceptions.UserAlreadyExists
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FireAuthRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

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

                        imageRef.putBytes(photoByte).addOnCompleteListener {
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
}