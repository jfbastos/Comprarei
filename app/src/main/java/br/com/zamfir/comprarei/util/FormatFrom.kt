package br.com.zamfir.comprarei.util

object FormatFrom {

    fun stringToInt(value : String) : Int{
      return try {
          value.toInt().takeIf { it > 0 } ?: 1
      }catch (e : Exception){
          1
      }
    }

    fun doubleToMonetary(currency : String, value : Double) : String{
        return "$currency ${String.format("%.2f",value)}"
    }
}