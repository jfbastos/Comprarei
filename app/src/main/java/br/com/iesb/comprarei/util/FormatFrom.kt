package br.com.iesb.comprarei.util

import java.lang.Exception
import java.math.BigDecimal
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

    fun stringToDouble(value : String) : Double{
        return try{
            value.toDouble()
        }catch (e : Exception){
            0.0
        }
    }

    fun stringToInt(value : String) : Int{
        return try {
            value.toInt()
        }catch (e : Exception){
            0
        }
    }

}