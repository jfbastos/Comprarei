package br.com.zamfir.comprarei.core.database.dao

import androidx.room.*
import br.com.zamfir.comprarei.core.database.entity.Product
import kotlinx.coroutines.flow.Flow

/**
 * Funções suspend e Flow são usadas pelas telas; as bloqueantes ficam para quem roda dentro de
 * runInTransaction (backup, restauração, exclusão de compras), onde suspend não é permitido.
 */
@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product : Product) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products : List<Product>)

    /** Produtos da compra na ordem salva; emite de novo a cada alteração na tabela. */
    @Query("SELECT * FROM PRODUCTS WHERE cartId = :cartId ORDER BY position")
    fun observeProducts(cartId : Int) : Flow<List<Product>>

    @Query("SELECT * FROM PRODUCTS WHERE cartId = :cartId ORDER BY position")
    fun getProducts(cartId : Int) : List<Product>

    @get: Query("SELECT * FROM PRODUCTS")
    val allProducts : List<Product>

    @Query("DELETE FROM PRODUCTS WHERE id IN (:ids)")
    suspend fun delete(ids : List<Int>)

    @Query("DELETE FROM PRODUCTS")
    fun deleteAll()

    @Query("DELETE FROM PRODUCTS WHERE cartId = :cartId")
    fun deleteProductsFromCart(cartId : Int) : Int

    @Update
    suspend fun updateProduct(product : Product)

    @Update
    suspend fun updateOrder(reorderedList : List<Product>)

    /**
     * Progresso e valor total de cada compra que tem produtos (compras vazias não aparecem).
     * Emite de novo a cada alteração em PRODUCTS.
     */
    @Query(
        "SELECT cartId, COUNT(*) AS total, SUM(CASE WHEN done THEN 1 ELSE 0 END) AS done, " +
            "SUM(price * quantity) AS totalValue FROM PRODUCTS GROUP BY cartId"
    )
    fun observeProgressByCart(): Flow<List<CartProgress>>

    @Query("UPDATE PRODUCTS SET done=:isDone WHERE id = :id")
    suspend fun updateDone(isDone : Boolean, id : Int)

}
