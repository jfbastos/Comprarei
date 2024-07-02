package br.com.zamfir.comprarei.view.listeners

import java.lang.Exception

interface LoginWithGoogleListener {
    fun userLoggedIn()
    fun loginError(exception: Exception?)

    companion object{
        lateinit var loginListener: LoginWithGoogleListener
        fun setOnListener(loginListener : LoginWithGoogleListener){
            this.loginListener = loginListener
        }
    }


}