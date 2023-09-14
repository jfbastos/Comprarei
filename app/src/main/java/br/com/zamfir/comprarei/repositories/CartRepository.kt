package br.com.zamfir.comprarei.repositories

import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.dao.CartDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CartRepository(private val cartDao: CartDao, private val dispatcher: CoroutineDispatcher) {

    suspend fun getCarts() = withContext(dispatcher) {cartDao.carts}

    suspend fun saveCart(cart: Cart) = withContext(dispatcher) { cartDao.insertCart(cart) }

    suspend fun deleteCart(cartId: Int) = withContext(dispatcher) { cartDao.delete(cartId) }

    suspend fun updateTotal(total: String, id: Int) = withContext(dispatcher) { cartDao.updateTotal(total, id) }

    suspend fun updateCart(cart: Cart) = withContext(dispatcher) { cartDao.updateCart(cart) }

    suspend fun updateAll(cartList: List<Cart>) = withContext(dispatcher) { cartDao.insertAll(cartList) }

    suspend fun updatePosition(carts : List<Cart>) =  withContext(dispatcher) {cartDao.updateOrder(carts)}

    suspend fun deleteAll() : Int = withContext(dispatcher) { cartDao.deleteAll() }

    suspend fun deleteCarts(carts: List<Int>) = withContext(dispatcher) {
        cartDao.deleteCarts(carts)
    }

}
