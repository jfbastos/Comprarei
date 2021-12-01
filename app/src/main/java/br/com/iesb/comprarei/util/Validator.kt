package br.com.iesb.comprarei.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object  Validator {

    fun validateDate(date: String): Boolean {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        sdf.isLenient = false
        return try {
            if(date.length < 10) return false
            val maxDate = sdf.parse("31/12/2050")
            sdf.parse(date).before(Date(maxDate.time))
        } catch (e: ParseException) {
            false
        }
    }
}