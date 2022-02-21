package br.com.iesb.comprarei.model

import androidx.lifecycle.LiveData
import br.com.iesb.comprarei.model.dao.CartDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CartRepository(private val cartDao : CartDao) {

    fun getCarts(): LiveData<List<Cart>> {
        return cartDao.carts
    }

    fun saveCart(name : String, data : String){
        val id = UUID.randomUUID().toString()
        val cart = Cart(id, name, data, "R$ 0,00")
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default){
                cartDao.insertCart(cart)
            }
        }
    }

    fun deleteCart(cartId : String){
        CoroutineScope(Dispatchers.IO).launch {
            cartDao.delete(cartId)
        }
    }

    fun updateTotal(total : String, id: String){
        CoroutineScope(Dispatchers.IO).launch {
            cartDao.updateTotal(total, id)
        }
    }






}
