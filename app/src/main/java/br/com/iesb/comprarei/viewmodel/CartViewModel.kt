package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.ViewModel
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.CartRepository

class CartViewModel(private val repository: CartRepository) : ViewModel() {

    val listOfCarts = repository.getCarts()

    fun saveCart(name : String, date : String){
        repository.saveCart(name, date)
    }

    fun deleteCart(cart : Cart){
        repository.deleteCart(cart.id)
    }

    fun updateTotal(total : String, id : String){
        repository.updateTotal(total, id)
    }

    fun sortList(option : Int, list : List<Cart>) : List<Cart>{
        return when (option) {
            0 -> {
                list.sortedBy { it.name }
            }
            1 -> {
                list.sortedBy { it.data }
            }
            else -> {
                list
            }
        }
    }

}