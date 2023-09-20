package br.com.zamfir.comprarei.view.listeners

interface InfoUpdateListener {
    fun infoUpdated()

    companion object{
        lateinit var infoUpdateListener : InfoUpdateListener
        fun setOnListener(infoUpdate : InfoUpdateListener){
            infoUpdateListener = infoUpdate
        }
    }
}