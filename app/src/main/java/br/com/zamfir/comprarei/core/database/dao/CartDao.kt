package br.com.zamfir.comprarei.core.database.dao

import androidx.room.*
import br.com.zamfir.comprarei.core.database.entity.Cart
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCart(cart : Cart) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(cartList : List<Cart>)

    @get: Query("SELECT * FROM CARTS ORDER BY position")
    val carts: List<Cart>

    /** Compras na ordem salva; emite de novo a cada alteração na tabela. */
    @Query("SELECT * FROM CARTS ORDER BY position")
    fun observeCarts(): Flow<List<Cart>>

    @Query("DELETE FROM CARTS WHERE id = :id")
    fun delete(id : Int)

    @Query("DELETE FROM CARTS")
    fun deleteAll() : Int

    @Query("UPDATE CARTS SET total = :total WHERE id = :id")
    fun updateTotal(total : String, id : Int)

    @Update
    suspend fun updateCart(cart : Cart) : Int

    @Update
    suspend fun updateOrder(carts : List<Cart>)

    /**
     * Quantas compras usam cada categoria (só aparecem categorias com ao menos uma compra).
     * Emite de novo a cada alteração em CARTS.
     */
    @Query("SELECT categoryId, COUNT(*) AS cartCount FROM CARTS WHERE categoryId != 0 GROUP BY categoryId")
    fun observeCountByCategory(): Flow<List<CategoryUsage>>

    @Query("SELECT COUNT(*) FROM CARTS WHERE categoryId = :categoryId")
    fun countWithCategory(categoryId: Int): Int

    /** Deixa sem categoria as compras que usavam [categoryId]. */
    @Query("UPDATE CARTS SET categoryId = 0 WHERE categoryId = :categoryId")
    fun clearCategory(categoryId: Int)

    /** Lojas já usadas, sem repetição, para sugerir no formulário de carrinho. */
    @Query("SELECT DISTINCT store FROM CARTS WHERE TRIM(store) != '' ORDER BY store COLLATE NOCASE")
    fun getStores(): List<String>

    @Query("DELETE FROM CARTS WHERE id IN (:carts)")
    fun deleteCarts(carts: List<Int>)
}