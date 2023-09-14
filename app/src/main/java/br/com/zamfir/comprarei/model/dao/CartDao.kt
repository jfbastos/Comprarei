package br.com.zamfir.comprarei.model.dao

import androidx.room.*
import br.com.zamfir.comprarei.model.entity.Cart

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCart(cart : Cart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(cartList : List<Cart>)

    @get: Query("SELECT * FROM carts ORDER BY position")
    val carts: List<Cart>

    @Query("DELETE FROM carts WHERE id = :id")
    fun delete(id : Int)

    @Query("DELETE FROM carts")
    fun deleteAll() : Int

    @Query("UPDATE carts SET total = :total WHERE id = :id")
    fun updateTotal(total : String, id : Int)

    @Update
    fun updateCart(cart : Cart) : Int

    @Update
    fun updateOrder(carts : List<Cart>)

    @Query("DELETE FROM carts WHERE id IN (:carts)")
    fun deleteCarts(carts: List<Int>)
}