package br.com.iesb.comprarei.model

import androidx.lifecycle.LiveData
import br.com.iesb.comprarei.model.dao.ProductDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductRepository(private val productDao : ProductDao) {

    fun getProducts(cartId : String) : LiveData<List<Product>> = productDao.getProducts(cartId)

    fun saveProduct(product: Product){
        CoroutineScope(Dispatchers.IO).launch {
            productDao.insertProduct(product)
        }
    }

    fun updateProduct(product : Product){
        CoroutineScope(Dispatchers.IO).launch {
            productDao.updateProduct(product)
        }
    }

    fun deleteProduct(id : Int) {
        CoroutineScope(Dispatchers.IO).launch {
            productDao.delete(id)
        }
    }

}