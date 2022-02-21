package br.com.iesb.comprarei.util

import java.lang.Exception
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

object FormatFrom {

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

    fun doubleToMonetary(currency : String, value : Double) : String{
        return "$currency ${String.format("%.2f",value)}"
    }

}