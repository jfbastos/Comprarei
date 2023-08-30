package br.com.iesb.comprarei.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*


class MoneyTextWatcher(editText: EditText?) : TextWatcher {
    private val editTextWeakReference: WeakReference<EditText>
    private val locale = Locale("Pt", "BR")

    init {
        editTextWeakReference = WeakReference<EditText>(editText)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(editable: Editable) {
        val editText: EditText = editTextWeakReference.get() ?: return
        editText.removeTextChangedListener(this)
        val parsed: BigDecimal = parseToBigDecimal(editable.toString(), locale)
        val formatted: String = NumberFormat.getCurrencyInstance(locale).format(parsed)
        editText.setText(formatted)
        editText.setSelection(formatted.length)
        editText.addTextChangedListener(this)
    }

    private fun parseToBigDecimal(value: String, locale: Locale): BigDecimal {
        val replaceable = String.format(
            "[%s,.\\s]",
            NumberFormat.getCurrencyInstance(locale).currency?.symbol ?: BigDecimal(0)
        )
        val cleanString = value.replace(replaceable.toRegex(), "")
        return BigDecimal(cleanString).setScale(
            2, BigDecimal.ROUND_FLOOR
        ).divide(
            BigDecimal(100), BigDecimal.ROUND_FLOOR
        )
    }


}