package br.com.zamfir.comprarei.view.listeners

import android.net.Uri

interface PhotoSelectedListener {
    fun onPhotoSelected(uri: Uri)

    companion object{
        lateinit var photoSelectedListener : PhotoSelectedListener
        fun setOnListener(photoSelectedListener: PhotoSelectedListener){
            this.photoSelectedListener = photoSelectedListener
        }
    }
}