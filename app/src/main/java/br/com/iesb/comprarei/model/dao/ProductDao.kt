package br.com.iesb.comprarei.model.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.model.Product
import br.com.iesb.comprarei.viewmodel.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertProduct(product : Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products : List<Product>)

    @Query("SELECT * FROM products WHERE cartId = :cartId")
    fun getProducts(cartId : Int) : Flow<List<Product>>

    @get: Query("SELECT * FROM products")
    val allProducts : Flow<List<Product>>

    @Query("DELETE FROM products WHERE id = :id")
    fun delete(id : Int)

    @Query("DELETE FROM products WHERE cartId = :cartId")
    fun deleteProductsFromCart(cartId : Int) : Int

    @Update
    fun updateProduct(product : Product)

    @Query("UPDATE products SET done=:isDone WHERE id = :id")
    fun updateDone(isDone : Boolean, id : Int)

}