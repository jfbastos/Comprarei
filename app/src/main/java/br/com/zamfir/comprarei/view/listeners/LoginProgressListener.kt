package br.com.zamfir.comprarei.view.listeners

interface LoginProgressListener {

    fun onProgress(progressInfo : String)
    fun onFinish()

    companion object{
        lateinit var loginProgressListener: LoginProgressListener
        fun setOnListener(loginProgress: LoginProgressListener){
            loginProgressListener = loginProgress
        }
    }

}