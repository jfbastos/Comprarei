package br.com.zamfir.comprarei.viewmodel.states

data class LoginState (
    val loading : Boolean = false,
    val success : Boolean = false,
    val msgError : String? = null,
    val error : Exception? = null
)