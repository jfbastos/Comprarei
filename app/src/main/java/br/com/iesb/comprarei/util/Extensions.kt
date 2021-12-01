package br.com.iesb.comprarei.util

import android.view.View
import android.view.animation.AnimationUtils
import br.com.iesb.comprarei.R
import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.errorAnimation(){
    val error = AnimationUtils.loadAnimation(this.context, R.anim.shake)
    this.startAnimation(error)
}

fun View.setVisibility(visibiliy : Boolean){
    if(visibiliy){
        this.visibility = View.VISIBLE
    }else{
        this.visibility = View.GONE
    }
}