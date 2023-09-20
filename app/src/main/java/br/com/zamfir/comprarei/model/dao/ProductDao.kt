package br.com.zamfir.comprarei.model.dao

import androidx.room.*
import br.com.zamfir.comprarei.model.entity.Product

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertProduct(product : Product) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products : List<Product>)

    @Query("SELECT * FROM products WHERE cartId = :cartId ORDER BY position")
    fun getProducts(cartId : Int) : List<Product>

    @get: Query("SELECT * FROM products")
    val allProducts : List<Product>

    @Query("DELETE FROM products WHERE id IN (:ids)")
    fun delete(ids : List<Int>)

    @Query("DELETE FROM products WHERE cartId = :cartId")
    fun deleteProductsFromCart(cartId : Int) : Int

    @Update
    fun updateProduct(product : Product)

    @Update
    fun updateOrder(reorderedList : List<Product>)

    @Query("UPDATE products SET done=:isDone WHERE id = :id")
    fun updateDone(isDone : Boolean, id : Int)

}