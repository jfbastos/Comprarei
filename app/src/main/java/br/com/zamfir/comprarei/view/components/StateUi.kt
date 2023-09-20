package br.com.zamfir.comprarei.view.components

data class StateUi(
    var result : Any? = null,
    var messageError : String? = null,
    var isLoading : Boolean = false
)