package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.*
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.repositories.ProductRepository
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.viewmodel.states.SaveState
import kotlinx.coroutines.launch

class ProductViewModel(private val productRepository: ProductRepository) : ViewModel() {

    private var _products = MutableLiveData<List<Product>>()
    val products : LiveData<List<Product>> get() = _products

    private var _saveState = MutableLiveData<SaveState>()
    val saveState : LiveData<SaveState> get() = _saveState

    fun getProducts(cartId : Int) = viewModelScope.launch {
        _products.value = productRepository.getProducts(cartId)
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
        }
    }

    fun updateDone(isDone: Boolean, id: Int) {
        viewModelScope.launch {
            productRepository.updateDone(isDone, id)
        }
    }

    fun saveProduct(product: Product) = viewModelScope.launch {
        val id = productRepository.saveProduct(product)
        _saveState.value = SaveState(savedId = id , savedItem = product)
    }

    fun deleteProduct(ids : List<Int>) {
        viewModelScope.launch {
            productRepository.deleteProduct(ids)
        }
    }

    fun sortList(option: String, list: List<Product>): List<Product> {
        return when (option) {
            Constants.FILTER_NAME -> {
                list.sortedBy { it.name }
            }
            Constants.FILTER_QUANTITY -> {
                list.sortedBy { it.price }
            }
            Constants.FILTER_VALUE_HIGH  -> {
                list.sortedBy { it.price }.reversed()
            }
            Constants.FILTER_VALUE_LOW -> {
                list.sortedBy { it.price }
            }
            Constants.FILTER_UNDONE -> {
                list.sortedBy { it.done }
            }
            Constants.FILTER_DONE -> {
                list.sortedBy { it.done }.reversed()
            }
            else -> {
                list
            }
        }
    }

    fun updateOrder(reorderedList: List<Product>) = viewModelScope.launch{
        reorderedList.forEachIndexed { index, product ->
            product.position = index
        }
        productRepository.updateOrder(reorderedList)
    }

}