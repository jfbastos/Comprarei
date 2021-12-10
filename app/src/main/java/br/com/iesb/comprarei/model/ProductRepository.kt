package br.com.iesb.comprarei.model

import androidx.lifecycle.LiveData
import br.com.iesb.comprarei.MyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ProductRepository {

    fun getProducts(cartId : String) : List<Product> = MyApplication.database!!.ProductDao().getProducts(cartId)

    fun saveProduct(product: Product){
        CoroutineScope(Dispatchers.IO).launch {
            MyApplication.database!!.ProductDao().insertProduct(product)
        }
    }

    fun updateProduct(product : Product){
        CoroutineScope(Dispatchers.IO).launch {
            MyApplication.database!!.ProductDao().updateProduct(product)
        }
    }

    fun deleteProduct(id : Int) {
        CoroutineScope(Dispatchers.IO).launch {
            MyApplication.database!!.ProductDao().delete(id)
        }
    }

}