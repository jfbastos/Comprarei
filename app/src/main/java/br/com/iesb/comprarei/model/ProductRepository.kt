package br.com.iesb.comprarei.model

import androidx.lifecycle.LiveData
import br.com.iesb.comprarei.di.dispachersModule
import br.com.iesb.comprarei.model.dao.ProductDao
import kotlinx.coroutines.*

class ProductRepository(private val productDao : ProductDao, private val dispacher : CoroutineDispatcher) {

    fun getProducts(cartId : String) : LiveData<List<Product>> = productDao.getProducts(cartId)

    suspend fun saveProduct(product: Product) = withContext(dispacher) {  productDao.insertProduct(product) }

    suspend fun updateProduct(product : Product) = withContext(dispacher) { productDao.updateProduct(product) }

    suspend fun deleteProducts(cartId : String) = withContext(dispacher) { productDao.deleteProductsFromCart(cartId) }

    suspend fun deleteProduct(id : Int) = withContext(dispacher) { productDao.delete(id) }

    suspend fun updateDone(isDone : Boolean, id : Int) = withContext(dispacher) {productDao.updateDone(isDone, id)}

}