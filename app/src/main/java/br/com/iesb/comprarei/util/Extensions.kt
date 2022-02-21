package br.com.iesb.comprarei.util

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
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

fun MenuItem.toggleVisibility(){
    this.isVisible = !this.isVisible
}

fun android.widget.SearchView.show(context: Context){
    this.requestFocus()
    showKeyboard(context)
    this.setQuery("", false)
}


fun showKeyboard(context: Context) {
    val inputMethodManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}