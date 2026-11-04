package br.com.zamfir.comprarei.core.presentation.states

import java.lang.Exception

data class DeleteState(
    val canDelete : Boolean = true,
    val itemDeleted : Any? = null,
    val error : Exception? = null
)