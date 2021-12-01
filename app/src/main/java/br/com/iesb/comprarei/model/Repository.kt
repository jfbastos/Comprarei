package br.com.iesb.comprarei.model

import androidx.lifecycle.LiveData
import br.com.iesb.comprarei.MyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class Repository {

    fun getCarts(): LiveData<List<Cart>> {
        return MyApplication.database!!.CartDao().carts
    }

    fun saveCart(name : String, data : String){
        val id = UUID.randomUUID().toString()
        val cart = Cart(id, name, data)
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default){
                MyApplication.database!!.CartDao().insertCart(cart)
            }
        }
    }

    fun deleteCart(cartId : String){
        CoroutineScope(Dispatchers.IO).launch {
            MyApplication.database!!.CartDao().delete(cartId)
        }
    }



}
