package br.com.zamfir.comprarei.view.listeners

interface PhotopickerListener {
    fun onPhotoClicked()

    companion object{
        lateinit var photopickerListener : PhotopickerListener
        fun setOnListener(photopickerListener: PhotopickerListener){
            this.photopickerListener = photopickerListener
        }
    }
}