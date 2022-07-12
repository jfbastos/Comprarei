package br.com.iesb.comprarei.util

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
            if(value.toInt() == 0) 1 else value.toInt()
        }catch (e : Exception){
            1
        }
    }

    fun doubleToMonetary(currency : String, value : Double) : String{
        return "$currency ${String.format("%.2f",value)}"
    }

}