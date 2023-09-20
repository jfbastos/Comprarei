package br.com.zamfir.comprarei.util

import android.content.Context
import android.text.Editable
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import br.com.zamfir.comprarei.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.lang.StringBuilder

fun TextInputEditText.errorAnimation() {
    val error = AnimationUtils.loadAnimation(this.context, R.anim.shake)
    this.startAnimation(error)
}

fun TextInputLayout.errorAnimation() {
    val error = AnimationUtils.loadAnimation(this.context, R.anim.shake)
    this.startAnimation(error)
}

fun View.setVisibility(visibility: Boolean) {
    if (visibility) this.visibility = View.VISIBLE else this.visibility = View.GONE
}

fun MenuItem.toggleVisibility() {
    this.isVisible = !this.isVisible
}

fun android.widget.SearchView.show(context: Context) {
    this.requestFocus()
    showKeyboard(context)
    this.setQuery("", false)
}

fun showKeyboard(context: Context) {
    val inputMethodManager: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun Editable?.convertMonetaryToDouble(): Double {
    if (this.isNullOrEmpty()) return 0.0
    val valueWithoutDot = removeDot(this.toString())
    return try {
        val text: String = if (valueWithoutDot.contains(',')) {
            valueWithoutDot.substring(2, valueWithoutDot.length).replace(',', '.')
        } else {
            valueWithoutDot.substring(2, valueWithoutDot.length)
        }
        text.trim().toDouble()
    } catch (e: Exception) {
        Log.e("Erro -> ConvertMonetary", "Erro ao converter para moeda. Erro : ${e.message}")
        0.0
    }
}

fun String?.convertMonetaryToDouble(): Double {
    if (this.isNullOrEmpty()) return 0.0
    val valueWithoutDot = removeDot(this.toString())
    return try {
        val text: String = if (valueWithoutDot.contains(',')) {
            valueWithoutDot.substring(2, valueWithoutDot.length).replace(',', '.')
        } else {
            valueWithoutDot.substring(2, valueWithoutDot.length)
        }
        text.trim().toDouble()
    } catch (e: Exception) {
        Log.e("Erro -> ConvertMonetary", "Erro ao converter para moeda. Erro : ${e.message}")
        0.0
    }
}

private fun removeDot(value : String) : String{
    return StringBuilder(value).apply {
        val indexOfDot = this.indexOf(".")
        if(indexOfDot >= 0) this.deleteCharAt(indexOfDot) else this
    }.toString()
}

