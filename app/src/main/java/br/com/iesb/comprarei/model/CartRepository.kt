package br.com.iesb.comprarei.model

import br.com.iesb.comprarei.interfaces.TaskListener
import br.com.iesb.comprarei.model.dao.CartDao
import br.com.iesb.comprarei.util.Constants
import kotlinx.coroutines.*
import java.util.*

class CartRepository(private val cartDao : CartDao, private val dispatcher: CoroutineDispatcher) {

    fun getCarts(taskListener: TaskListener) {
        try{
            taskListener.onSuccess(cartDao.carts)
        }catch (e : Exception){
            taskListener.onError(e)
        }
    }

    suspend fun saveCart(name : String, data : String, taskListener: TaskListener) = withContext(dispatcher) {
        try{
            val id = UUID.randomUUID().toString()
            val cart = Cart(id, name, data, Constants.EMPTY_CART_VALUE)
            cartDao.insertCart(cart)
            taskListener.onSuccess(true)
        }catch (e : Exception){
            taskListener.onError(e)
        }

    }

    suspend fun deleteCart(cartId : String, taskListener: TaskListener) = withContext(dispatcher) {
        try{
            cartDao.delete(cartId)
            taskListener.onSuccess(true)
        }catch (e : Exception){
            taskListener.onError(e)
        }

    }

    suspend fun updateTotal(total : String, id: String, taskListener: TaskListener) = withContext(dispatcher) {
        try{
            cartDao.updateTotal(total, id)
            taskListener.onSuccess(true)
        }catch (e : Exception){
            taskListener.onError(e)
        }

    }

}
