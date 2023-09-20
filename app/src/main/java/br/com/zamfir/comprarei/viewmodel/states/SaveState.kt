package br.com.zamfir.comprarei.viewmodel.states

import java.lang.Exception

data class SaveState(
    val loading : Boolean = false,
    val savedId : Long = -1,
    val savedItem : Any? = null,
    val error : Exception? = null
)