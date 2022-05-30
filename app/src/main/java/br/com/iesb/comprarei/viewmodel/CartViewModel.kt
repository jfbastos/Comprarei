package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.iesb.comprarei.R
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository) : ViewModel() {

    val listOfCarts = repository.getCarts()

    fun saveCart(name : String, date : String) {
        viewModelScope.launch {
            repository.saveCart(name, date)
        }

    }

    fun deleteCart(cart : Cart){
        viewModelScope.launch{
            repository.deleteCart(cart.id)
            productsRepository.deleteProducts(cart.id)
        }

    }

    fun updateTotal(total : String, id : String){
        viewModelScope.launch {
            repository.updateTotal(total, id)
        }

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