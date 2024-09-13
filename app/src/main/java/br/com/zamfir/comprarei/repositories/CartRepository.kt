package br.com.zamfir.comprarei.repositories

import br.com.zamfir.comprarei.model.AppDatabase
import br.com.zamfir.comprarei.model.entity.Cart
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CartRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    suspend fun getCarts() = withContext(dispatcher) { appDatabase.CartDao().carts }

    suspend fun saveCart(cart: Cart): Long = withContext(dispatcher) { appDatabase.CartDao().insertCart(cart) }

    suspend fun updateTotal(total: String, id: Int) = withContext(dispatcher) { appDatabase.CartDao().updateTotal(total, id) }

    suspend fun updateCart(cart: Cart) = withContext(dispatcher) { appDatabase.CartDao().updateCart(cart) }

    suspend fun updatePosition(carts: List<Cart>) = withContext(dispatcher) { appDatabase.CartDao().updateOrder(carts) }

    suspend fun deleteCarts(carts: List<Int>) = withContext(dispatcher) { appDatabase.CartDao().deleteCarts(carts) }

    suspend fun getCategories() = withContext(dispatcher) { appDatabase.CategoryDao().allCategories }

    suspend fun getCategoryById(id: Int) = withContext(dispatcher) { appDatabase.CategoryDao().getCategoryById(id) }
}
