package br.com.zamfir.comprarei.viewmodel.states

import br.com.zamfir.comprarei.data.model.entity.Cart
import br.com.zamfir.comprarei.data.model.entity.Category
import br.com.zamfir.comprarei.data.model.entity.UserInfo
import com.google.firebase.firestore.auth.User
import java.lang.Exception

data class CartsState(
    val loading : Boolean = false,
    val carts : List<Cart>? = listOf(),
    val categories : List<Category> = listOf(),
    val isShowTotal : Boolean = true,
    val error : Exception? = null,
    val loggedUser : UserInfo? = null
)