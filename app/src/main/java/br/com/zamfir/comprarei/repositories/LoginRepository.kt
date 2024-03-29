package br.com.zamfir.comprarei.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginRepository(private val dispatcher: CoroutineDispatcher) {

    private var auth : FirebaseAuth = Firebase.auth

    @Throws
    suspend fun loginUser(email : String, password : String) = withContext(dispatcher){
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
        }.onSuccess {
            return@withContext it.user
        }.onFailure {
            throw it
        }







    }

}