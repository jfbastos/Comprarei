package br.com.zamfir.comprarei.data.model.dao

import androidx.room.*
import br.com.zamfir.comprarei.data.model.entity.Cart

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCart(cart : Cart) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(cartList : List<Cart>)

    @get: Query("SELECT * FROM CARTS ORDER BY position")
    val carts: List<Cart>

    @Query("DELETE FROM CARTS WHERE id = :id")
    fun delete(id : Int)

    @Query("DELETE FROM CARTS")
    fun deleteAll() : Int

    @Query("UPDATE CARTS SET total = :total WHERE id = :id")
    fun updateTotal(total : String, id : Int)

    @Update
    fun updateCart(cart : Cart) : Int

    @Update
    fun updateOrder(carts : List<Cart>)

    @Query("DELETE FROM CARTS WHERE id IN (:carts)")
    fun deleteCarts(carts: List<Int>)
}