package br.com.zamfir.comprarei.core.util

import android.text.Editable
import br.com.zamfir.comprarei.core.util.log.AppLogger

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
        AppLogger.warning("Falha ao converter valor monetário \"$this\": ${e.message}")
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
        AppLogger.warning("Falha ao converter valor monetário \"$this\": ${e.message}")
        0.0
    }
}

private fun removeDot(value : String) : String{
    return StringBuilder(value).apply {
        val indexOfDot = this.indexOf(".")
        if(indexOfDot >= 0) this.deleteCharAt(indexOfDot) else this
    }.toString()
}
