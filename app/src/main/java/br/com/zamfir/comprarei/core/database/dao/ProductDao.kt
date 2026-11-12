package br.com.zamfir.comprarei.core.database.dao

import androidx.room.*
import br.com.zamfir.comprarei.core.database.entity.Product

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertProduct(product : Product) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products : List<Product>)

    @Query("SELECT * FROM PRODUCTS WHERE cartId = :cartId ORDER BY position")
    fun getProducts(cartId : Int) : List<Product>

    @get: Query("SELECT * FROM PRODUCTS")
    val allProducts : List<Product>

    @Query("DELETE FROM PRODUCTS WHERE id IN (:ids)")
    fun delete(ids : List<Int>)

    @Query("DELETE FROM PRODUCTS WHERE cartId = :cartId")
    fun deleteProductsFromCart(cartId : Int) : Int

    @Update
    fun updateProduct(product : Product)

    @Update
    fun updateOrder(reorderedList : List<Product>)

    /** Progresso de cada compra que tem produtos (compras vazias não aparecem). */
    @Query("SELECT cartId, COUNT(*) AS total, SUM(CASE WHEN done THEN 1 ELSE 0 END) AS done FROM PRODUCTS GROUP BY cartId")
    fun progressByCart(): List<CartProgress>

    @Query("UPDATE PRODUCTS SET done=:isDone WHERE id = :id")
    fun updateDone(isDone : Boolean, id : Int)

}