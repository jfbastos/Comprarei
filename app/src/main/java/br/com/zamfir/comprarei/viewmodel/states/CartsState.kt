package br.com.zamfir.comprarei.viewmodel.states

import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import java.lang.Exception

data class CartsState(
    val loading : Boolean = false,
    val carts : List<Cart>? = listOf(),
    val categories : List<Category> = listOf(),
    val isShowTotal : Boolean = true,
    val error : Exception? = null
)