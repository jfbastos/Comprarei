package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.ViewModel
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.Repository

class MainViewModel(private val repository: Repository) : ViewModel() {

    val listOfCarts = repository.getCarts()

    fun saveCart(name : String, date : String){
        repository.saveCart(name, date)
    }

    fun deleteCart(cart : Cart){
        repository.deleteCart(cart.id)
    }

    fun sortList(option : String, list : List<Cart>) : List<Cart>{
        when (option) {
            "Name" -> {
               return list.sortedBy { it.name }
            }
            "Data" -> {
                return list.sortedBy { it.data }
            }
            else -> {
                return list
            }
        }
    }

}