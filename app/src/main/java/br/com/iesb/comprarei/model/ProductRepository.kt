package br.com.iesb.comprarei.model

import br.com.iesb.comprarei.model.dao.ProductDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProductRepository(private val productDao: ProductDao, private val dispacher: CoroutineDispatcher) {

    val products : Flow<List<Product>> = productDao.allProducts

    suspend fun saveProduct(product: Product) = withContext(dispacher) {
        productDao.insertProduct(product)
    }

    suspend fun saveAll(products: List<Product>) = withContext(dispacher) {
        products.forEach {
            productDao.insertProduct(it)
        }
    }

    suspend fun updateProduct(product: Product) = withContext(dispacher) { productDao.updateProduct(product) }

    suspend fun deleteProducts(cartId: Int): Boolean {
        return withContext(dispacher) {
            productDao.deleteProductsFromCart(cartId) > 0
        }
    }

    suspend fun deleteProduct(id: Int) = withContext(dispacher) { productDao.delete(id) }

    suspend fun updateDone(isDone: Boolean, id: Int) = withContext(dispacher) { productDao.updateDone(isDone, id) }

    suspend fun updateAll(currentList: List<Product>) = withContext(dispacher) { productDao.insertAll(currentList) }

}