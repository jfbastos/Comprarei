package br.com.zamfir.comprarei.core.presentation

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import br.com.zamfir.comprarei.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

fun TextInputEditText.errorAnimation(msg : String) {
    val error = AnimationUtils.loadAnimation(this.context, R.anim.shake)
    this.error = msg
    this.startAnimation(error)
}

fun TextInputLayout.errorAnimation(msg : String = "", usesPeek : Boolean = false) {
    val error = AnimationUtils.loadAnimation(this.context, R.anim.shake)
    this.error = msg
    if(usesPeek) this.errorIconDrawable = null
    this.isErrorEnabled = true
    this.requestFocus()
    this.startAnimation(error)
}

fun TextInputLayout.resetErrorAnimation(){
    this.isErrorEnabled = false
    this.error = null
}

fun View.isVisible(visibility: Boolean) {
    if (visibility) this.visibility = View.VISIBLE else this.visibility = View.GONE
}

fun View.setEnabled(){
    this.isEnabled = true
    this.alpha = 1f
}

fun View.setDisabled(){
    this.isEnabled = false
    this.alpha = 0.5f
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

fun TextInputEditText.setMonetary(initialValue : String){
    this.addTextChangedListener(MoneyTextWatcher(this))
    this.setText(initialValue)
}
