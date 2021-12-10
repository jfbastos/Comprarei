package br.com.iesb.comprarei.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.model.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductViewModel(private val cartRepository: ProductRepository) : ViewModel() {

    private val _productsLiveData = MutableLiveData<List<Product>>()
    val productsLiveData : LiveData<List<Product>> get() = _productsLiveData

    fun getProducts(cartId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _productsLiveData.postValue(cartRepository.getProducts(cartId))
        }
    }

    fun updateProduct(product : Product){
        cartRepository.updateProduct(product)
    }


    fun saveProduct(product: Product) {
        cartRepository.saveProduct(product)
    }

    fun deleteProduct(product: Product) {
        cartRepository.deleteProduct(product.id)
    }

    fun sortList(option: String, list: List<Product>): List<Product> {
        when (option) {
            "Name" -> {
                return list.sortedBy { it.name }
            }
            "Price" -> {
                return list.sortedBy { it.price }
            }
            "Total" -> {
                return list.sortedBy { (it.price * it.quantity) }
            }
            else -> {
                return list
            }
        }
    }

}