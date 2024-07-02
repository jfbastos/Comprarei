package br.com.zamfir.comprarei.util

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object  DateUtil {

    fun validateDate(date: String): Boolean {
        return try {
            if(date.isBlank()) return true
            val sdf = SimpleDateFormat(Constants.DATE_PATTERN, Locale("pt", "BR"))
            sdf.isLenient = false
            if(date.length < 10) return false
            dateInRange(sdf, date)
        } catch (e: ParseException) {
            false
        }
    }

    private fun dateInRange(sdf: SimpleDateFormat, date: String): Boolean {
        val maxDate = sdf.parse(Constants.MAX_DATE_VALID)
        val minDate = sdf.parse(Constants.MIN_DATE_VALID)

        return sdf.parse(date)!!.after(Date(minDate!!.time)) && sdf.parse(date)!!.before(Date(maxDate!!.time))
    }

    fun getTodayDate() : String{
        return try{
            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
        }catch (e : Exception){
            Log.e(this.javaClass.name, e.toString())
            ""
        }
    }

    fun formatDate(date : String) : String{
        return try{
            val localdate = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME)
            localdate.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"))
        }catch (e : Exception){
            Log.e(this.javaClass.name, e.toString())
            ""
        }
    }
}