package br.com.zamfir.comprarei.viewmodel.states

import java.lang.Exception

data class GetState(
    val loading : Boolean = false,
    val data : Any? = null,
    val error : Exception? = null
)