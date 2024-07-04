package br.com.zamfir.comprarei.repositories

import android.content.Context
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.model.dao.ProductDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ProductRepository(private val context : Context, private val productDao: ProductDao, private val dispacher: CoroutineDispatcher) {

    suspend fun getProducts(cartId: Int) = withContext(dispacher){ productDao.getProducts(cartId) }

    suspend fun saveProduct(product: Product) : Long = withContext(dispacher) {
        productDao.insertProduct(product)
    }

    suspend fun saveAll(products: List<Product>) = withContext(dispacher) {
        products.forEach {
            productDao.insertProduct(it)
        }
    }

    suspend fun updateProduct(product: Product) = withContext(dispacher) { productDao.updateProduct(product) }

    suspend fun deleteProducts(cartId: Int) = withContext(dispacher) {
            productDao.deleteProductsFromCart(cartId)
    }

    suspend fun deleteProduct(ids: List<Int>) = withContext(dispacher) { productDao.delete(ids) }

    suspend fun updateDone(isDone: Boolean, id: Int) = withContext(dispacher) { productDao.updateDone(isDone, id) }

    suspend fun updateAll(currentList: List<Product>) = withContext(dispacher) { productDao.insertAll(currentList) }

    suspend fun updateOrder(reorderedList: List<Product>) = withContext(dispacher) {
        productDao.updateOrder(reorderedList)
    }
}