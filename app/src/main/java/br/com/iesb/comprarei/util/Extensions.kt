package br.com.iesb.comprarei.util

import android.content.Context
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import br.com.iesb.comprarei.R
import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.errorAnimation() {
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
    return try {
        if (this.isNullOrEmpty()) return 0.0
        val text: String = if (this.contains(',')) {
            this.substring(2, this.length).replace(',', '.')
        } else {
            this.substring(2, this.length)
        }
        text.toDouble()
    } catch (e: Exception) {
        0.0
    }
}