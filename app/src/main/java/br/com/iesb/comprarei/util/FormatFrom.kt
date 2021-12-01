package br.com.iesb.comprarei.util

import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

object FormatFrom {

    fun stringToData(date : Date) : String{
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            sdf.format(date)
        }catch (e : Exception){
            return "No date"
        }
    }

}