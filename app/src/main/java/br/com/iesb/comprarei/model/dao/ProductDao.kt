package br.com.iesb.comprarei.model.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import br.com.iesb.comprarei.model.Product


@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProduct(product : Product)

    @Query("SELECT * FROM products WHERE cartId = :cartId")
    fun getProducts(cartId : String) : LiveData<List<Product>>

    @Query("DELETE FROM products WHERE id = :id")
    fun delete(id : Int)

    @Query("DELETE FROM products WHERE cartId = :cartId")
    fun deleteProductsFromCart(cartId : String)

    @Update
    fun updateProduct(product : Product)

    @Query("UPDATE products SET done=:isDone WHERE id = :id")
    fun updateDone(isDone : Boolean, id : Int)

}