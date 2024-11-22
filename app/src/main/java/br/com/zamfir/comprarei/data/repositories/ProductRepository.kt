package br.com.zamfir.comprarei.data.repositories

import br.com.zamfir.comprarei.data.model.dao.ProductDao
import br.com.zamfir.comprarei.data.model.entity.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ProductRepository(private val productDao: ProductDao, private val dispacher: CoroutineDispatcher) {

    suspend fun getProducts(cartId: Int) = withContext(dispacher){ productDao.getProducts(cartId) }

    suspend fun saveProduct(product: Product) : Long = withContext(dispacher) { productDao.insertProduct(product) }

    suspend fun updateProduct(product: Product) = withContext(dispacher) { productDao.updateProduct(product) }

    suspend fun deleteProducts(cartId: Int) = withContext(dispacher) { productDao.deleteProductsFromCart(cartId) }

    suspend fun deleteProduct(ids: List<Int>) = withContext(dispacher) { productDao.delete(ids) }

    suspend fun updateDone(isDone: Boolean, id: Int) = withContext(dispacher) { productDao.updateDone(isDone, id) }

    suspend fun updateOrder(reorderedList: List<Product>) = withContext(dispacher) { productDao.updateOrder(reorderedList) }
}