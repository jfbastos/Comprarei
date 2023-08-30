package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import br.com.iesb.comprarei.util.convertMonetaryToDouble
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository) : ViewModel() {

    private var _carts = MutableLiveData<List<Cart>>()
    val carts : LiveData<List<Cart>> get() = _carts

    fun getCarts() = viewModelScope.launch{
        _carts.value = repository.getCarts()
    }

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

    fun deleteCarts(carts : List<Int>) = viewModelScope.launch{
        repository.deleteCarts(carts)
        carts.forEach {
            productsRepository.deleteProducts(it)
        }
    }

    fun updateTotal(total: String, id: Int) {
        viewModelScope.launch {
            repository.updateTotal(total, id)
        }
    }

    fun updateOrder(reorderedList : List<Cart>) {
        viewModelScope.launch {
            reorderedList.forEachIndexed { index, cart ->
                cart.position = index
            }
            repository.updatePosition(reorderedList)
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
            2 ->{
                list.sortedBy { it.total.convertMonetaryToDouble() }.reversed()
            }
            else -> {
                list
            }
        }
    }

}