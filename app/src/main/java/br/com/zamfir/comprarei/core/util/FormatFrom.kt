package br.com.zamfir.comprarei.core.util


object FormatFrom {

    fun doubleToMonetary(currency : String, value : Double) : String{
        return "$currency ${String.format("%.2f",value)}"
    }
}