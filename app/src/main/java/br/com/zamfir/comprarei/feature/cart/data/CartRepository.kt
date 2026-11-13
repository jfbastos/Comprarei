package br.com.zamfir.comprarei.feature.cart.data

import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.dao.CartProgress
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Compras. A Home observa os Flows e não precisa recarregar depois de gravar.
 * O dispatcher só é usado nas funções que chamam o DAO de forma bloqueante.
 */
class CartRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    fun observeCarts(): Flow<List<Cart>> = appDatabase.CartDao().observeCarts()

    /** Progresso e valor de cada compra; muda junto com os produtos. */
    fun observeProgress(): Flow<List<CartProgress>> = appDatabase.ProductDao().observeProgressByCart()

    fun observeCategories(): Flow<List<Category>> = appDatabase.CategoryDao().observeCategories()

    suspend fun saveCart(cart: Cart): Long = appDatabase.CartDao().insertCart(cart)

    suspend fun updateCart(cart: Cart) = appDatabase.CartDao().updateCart(cart)

    suspend fun updatePosition(carts: List<Cart>) = appDatabase.CartDao().updateOrder(carts)

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

    suspend fun getStores(): List<String> = withContext(dispatcher) { appDatabase.CartDao().getStores() }

    suspend fun getCategories() = withContext(dispatcher) { appDatabase.CategoryDao().allCategories }
}
