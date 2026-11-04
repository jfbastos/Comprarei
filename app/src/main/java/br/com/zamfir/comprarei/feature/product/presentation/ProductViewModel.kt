package br.com.zamfir.comprarei.feature.product.presentation

import androidx.lifecycle.*
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.product.ProductRepository
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.presentation.states.SaveState
import kotlinx.coroutines.launch

class ProductViewModel(private val productRepository: ProductRepository, private val configRepository: ConfigRepository) : ViewModel() {

    private var _products = MutableLiveData<ProductState>()
    val products : LiveData<ProductState> get() = _products

    private var _saveState = MutableLiveData<SaveState>()
    val saveState : LiveData<SaveState> get() = _saveState

    fun getProducts(cartId : Int) = viewModelScope.launch {
        val products =  productRepository.getProducts(cartId)
        val isMoveToBottom = configRepository.getIsToMoveToBottomDoneItens()

        _products.value = ProductState(products, isMoveToBottom)
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