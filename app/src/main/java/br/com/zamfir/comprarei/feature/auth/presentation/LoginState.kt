package br.com.zamfir.comprarei.feature.auth.presentation

import com.google.firebase.auth.FirebaseUser

data class LoginState (
    val loading : Boolean = false,
    val success : Boolean = false,
    val user : FirebaseUser? = null,
    val msgError : String? = null,
    val error : Exception? = null
)