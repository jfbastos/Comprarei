package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.*
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.model.ProductRepository
import br.com.iesb.comprarei.view.components.StateUi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductViewModel(private val productRepository: ProductRepository) : ViewModel() {

    var cartId : Int = -1

    val products : LiveData<List<Product>> = productRepository.products.map { it.filter { product -> product.cartId == cartId} }.asLiveData()

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

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            productRepository.saveProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product.id)
        }
    }

    fun reorderList(currentList: List<Product>) {
        viewModelScope.launch {
           productRepository.updateAll(currentList)
        }
    }

    fun deleteProducts(cartId: Int) {
        viewModelScope.launch {
            productRepository.deleteProducts(cartId)
        }
    }

    fun sortList(option: Int, list: List<Product>): List<Product> {
        return when (option) {
            0 -> {
                list.sortedBy { it.name }
            }
            1 -> {
                list.sortedBy { it.price }
            }
            2 -> {
                list.sortedBy { (it.price * it.quantity) }
            }
            else -> {
                list
            }
        }
    }

}