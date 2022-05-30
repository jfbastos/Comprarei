package br.com.iesb.comprarei.model

import androidx.lifecycle.LiveData
import br.com.iesb.comprarei.model.dao.CartDao
import kotlinx.coroutines.*
import java.util.*

class CartRepository(private val cartDao : CartDao, private val dispatcher: CoroutineDispatcher) {

    fun getCarts(): LiveData<List<Cart>> {
        return cartDao.carts
    }

    suspend fun saveCart(name : String, data : String) = withContext(dispatcher) {
        val id = UUID.randomUUID().toString()
        val cart = Cart(id, name, data, "R$ 0,00")
        cartDao.insertCart(cart)
    }

    suspend fun deleteCart(cartId : String) = withContext(dispatcher) {cartDao.delete(cartId) }

    suspend fun updateTotal(total : String, id: String) = withContext(dispatcher) { cartDao.updateTotal(total, id)}

}
