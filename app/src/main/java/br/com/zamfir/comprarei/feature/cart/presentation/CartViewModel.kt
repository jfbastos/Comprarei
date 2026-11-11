package br.com.zamfir.comprarei.feature.cart.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.feature.cart.data.CartRepository
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.product.ProductRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.presentation.states.DeleteState
import br.com.zamfir.comprarei.core.presentation.states.SaveState
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CartRepository, private val productsRepository: ProductRepository, private val configRepository: ConfigRepository, private val userRepository: UserRepository) : ViewModel() {

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

        val user = userRepository.getUserFromDb()

        val categories = repository.getCategories()

        _cartsState.value = CartsState(loading = false, carts = carts, categories = categories, isShowTotal = configRepository.getIsToShowTotalCart(), loggedUser = user)
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
}
