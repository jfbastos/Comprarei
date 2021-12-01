package br.com.iesb.comprarei.model.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.iesb.comprarei.model.Product


@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertProduct(product : Product)

    @get : Query("SELECT * FROM products")
    val products: LiveData<List<Product>>

}