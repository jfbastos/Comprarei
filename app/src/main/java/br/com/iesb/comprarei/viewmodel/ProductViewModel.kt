package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.model.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductViewModel(private val cartRepository: ProductRepository) : ViewModel() {

    //private val listOfProducts = MutableLiveData<List<Product>>()
    lateinit var productsLiveData: LiveData<List<Product>>

    fun getProducts(cartId: String) {
        viewModelScope.launch {
            productsLiveData = cartRepository.getProducts(cartId)
        }

    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            cartRepository.updateProduct(product)
        }
    }


    fun saveProduct(product: Product) {
        viewModelScope.launch {
            cartRepository.saveProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            cartRepository.deleteProduct(product.id)
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