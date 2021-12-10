package br.com.iesb.comprarei.util

import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import br.com.iesb.comprarei.R
import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.errorAnimation(msg : String){
    val error = AnimationUtils.loadAnimation(this.context, R.anim.shake)
    this.error = msg
    this.startAnimation(error)
}

fun View.setVisibility(visibiliy : Boolean){
    if(visibiliy){
        this.visibility = View.VISIBLE
    }else{
        this.visibility = View.GONE
    }
}

fun MenuItem.toggleVisibility(){
    this.isVisible = !this.isVisible
}