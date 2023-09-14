package br.com.zamfir.comprarei.viewmodel

import androidx.lifecycle.*
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.repositories.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(private val productRepository: ProductRepository) : ViewModel() {

    private var _products = MutableLiveData<List<Product>>()
    val products : LiveData<List<Product>> get() = _products

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

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            productRepository.saveProduct(product)
        }
    }

    fun deleteProduct(ids : List<Int>) {
        viewModelScope.launch {
            productRepository.deleteProduct(ids)
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

    fun updateOrder(reorderedList: List<Product>) = viewModelScope.launch{
        reorderedList.forEachIndexed { index, product ->
            product.position = index
        }
        productRepository.updateOrder(reorderedList)
    }

}