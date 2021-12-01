package br.com.iesb.comprarei.model.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.iesb.comprarei.model.Cart

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCart(cart : Cart)

    @get: Query("SELECT * FROM carts")
    val carts: LiveData<List<Cart>>

    @Query("DELETE FROM carts WHERE id = :id")
    fun delete(id : String)


}