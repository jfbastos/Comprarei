package br.com.zamfir.comprarei.core.presentation.listeners

interface InfoUpdateListener {
    fun infoUpdated()

    companion object{
        lateinit var infoUpdateListener : InfoUpdateListener
        fun setOnListener(infoUpdate : InfoUpdateListener){
            infoUpdateListener = infoUpdate
        }
    }
}