package br.com.zamfir.comprarei.feature.cart.data

import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.dao.CartProgress
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CartRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    suspend fun getCarts() = withContext(dispatcher) { appDatabase.CartDao().carts }

    suspend fun saveCart(cart: Cart): Long = withContext(dispatcher) { appDatabase.CartDao().insertCart(cart) }

    suspend fun updateCart(cart: Cart) = withContext(dispatcher) { appDatabase.CartDao().updateCart(cart) }

    suspend fun updatePosition(carts: List<Cart>) = withContext(dispatcher) { appDatabase.CartDao().updateOrder(carts) }

    /**
     * Exclui as compras e os produtos delas numa transação.
     * Devolve os produtos excluídos para que [restoreCarts] possa desfazer.
     */
    suspend fun deleteCarts(carts: List<Cart>): List<Product> = withContext(dispatcher) {
        appDatabase.runInTransaction<List<Product>> {
            val productDao = appDatabase.ProductDao()
            val products = carts.flatMap { productDao.getProducts(it.id) }
            carts.forEach { productDao.deleteProductsFromCart(it.id) }
            appDatabase.CartDao().deleteCarts(carts.map { it.id })
            products
        }
    }

    /** Desfaz [deleteCarts]: reinsere compras e produtos com os mesmos ids e firestoreUUID. */
    suspend fun restoreCarts(carts: List<Cart>, products: List<Product>) = withContext(dispatcher) {
        appDatabase.runInTransaction {
            appDatabase.CartDao().insertAll(carts)
            appDatabase.ProductDao().insertAll(products)
        }
    }

    suspend fun getProgress(): List<CartProgress> = withContext(dispatcher) { appDatabase.ProductDao().progressByCart() }

    suspend fun getStores(): List<String> = withContext(dispatcher) { appDatabase.CartDao().getStores() }

    suspend fun getCategories() = withContext(dispatcher) { appDatabase.CategoryDao().allCategories }
}
