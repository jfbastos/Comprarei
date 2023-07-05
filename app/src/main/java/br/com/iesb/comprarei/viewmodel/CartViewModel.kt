package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository) : ViewModel() {

    var carts: LiveData<List<Cart>> = repository.carts.asLiveData()

    fun saveCart(cart: Cart) {
        viewModelScope.launch {
            repository.saveCart(cart)
        }
    }

    fun deleteCart(cart: Cart) {
        viewModelScope.launch {
            repository.deleteCart(cart.id)
        }
    }

    fun updateTotal(total: String, id: Int) {
        viewModelScope.launch {
            repository.updateTotal(total, id)
        }
    }

    fun deleteAll(){
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun reorderList(currentList: List<Cart>) {
        viewModelScope.launch {
            repository.updateAll(currentList)
        }
    }

    fun updateCart(cart: Cart) {
        viewModelScope.launch {
            repository.updateCart(cart)
        }
    }

    fun sortList(option: Int, list: List<Cart>): List<Cart> {
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