package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.repositories.CartRepository
import br.com.zamfir.comprarei.repositories.ProductRepository
import br.com.zamfir.comprarei.util.convertMonetaryToDouble
import br.com.zamfir.comprarei.viewmodel.states.DeleteState
import br.com.zamfir.comprarei.viewmodel.states.CartsState
import br.com.zamfir.comprarei.viewmodel.states.SaveState
import kotlinx.coroutines.launch
import java.lang.Exception

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository) : ViewModel() {

    private var _cartsState = MutableLiveData<CartsState>()
    val cartsState : LiveData<CartsState> get() = _cartsState

    private var _deleteState = MutableLiveData<DeleteState>()
    val deleteState : LiveData<DeleteState> get() = _deleteState

    private var _saveState = MutableLiveData<SaveState>()
    val saveState : LiveData<SaveState> get() = _saveState

    fun saveCart(cart: Cart) = viewModelScope.launch {
        val id = repository.saveCart(cart)
        _saveState.value = SaveState(savedId = id, savedItem = cart)
    }

    fun updateAllData() = viewModelScope.launch {
        _cartsState.value = CartsState(loading = true)

        val carts = repository.getCarts().onEach {
            it.category = repository.getCategoryById(it.categoryId)
        }

        val categories = repository.getCategories()

        _cartsState.value = CartsState(loading = false, carts = carts, categories = categories)
    }

    fun deleteCarts(carts : List<Int>) = viewModelScope.launch{
        try{
            repository.deleteCarts(carts)
            carts.forEach {
                productsRepository.deleteProducts(it)
            }
        }catch (e : Exception){
            _deleteState.value = DeleteState(error = e)
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