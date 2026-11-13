package br.com.zamfir.comprarei.core.data.product

import android.content.Context
import androidx.room.withTransaction
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.util.formatMoney
import kotlinx.coroutines.flow.Flow

/**
 * Produtos de uma compra. As telas observam [observeProducts] e não precisam recarregar depois
 * de gravar: o Room emite a lista nova sozinho.
 *
 * Não usa dispatcher próprio: funções suspend e Flow do Room já rodam fora da thread principal.
 */
class ProductRepository(
    private val context: Context,
    private val appDatabase: AppDatabase,
) {

    private val productDao get() = appDatabase.ProductDao()

    fun observeProducts(cartId: Int): Flow<List<Product>> = productDao.observeProducts(cartId)

    suspend fun saveProduct(product: Product): Long = appDatabase.withTransaction {
        productDao.insertProduct(product).also { updateCartTotal(product.cartId) }
    }

    suspend fun updateProduct(product: Product) = appDatabase.withTransaction {
        productDao.updateProduct(product)
        updateCartTotal(product.cartId)
    }

    suspend fun deleteProducts(products: List<Product>) = appDatabase.withTransaction {
        productDao.delete(products.map { it.id })
        products.map { it.cartId }.distinct().forEach { updateCartTotal(it) }
    }

    /** Desfaz uma exclusão: reinsere os produtos com os mesmos ids, posições e firestoreUUID. */
    suspend fun restoreProducts(products: List<Product>) = appDatabase.withTransaction {
        productDao.insertAll(products)
        products.map { it.cartId }.distinct().forEach { updateCartTotal(it) }
    }

    suspend fun updateDone(isDone: Boolean, id: Int) = productDao.updateDone(isDone, id)

    suspend fun updateOrder(reorderedList: List<Product>) = productDao.updateOrder(reorderedList)

    /*
     * O app calcula o total a partir dos produtos (ProductDao.observeProgressByCart) e não lê este campo.
     * Ele continua sendo gravado só para os documentos do Firestore seguirem compatíveis com
     * versões antigas do app em outros aparelhos, que exibem esse texto.
     * Roda na mesma transação da alteração, para o texto nunca ficar diferente dos produtos.
     */
    private fun updateCartTotal(cartId: Int) {
        val total = productDao.getProducts(cartId).sumOf { it.price * it.quantity }
        appDatabase.CartDao().updateTotal(formatMoney(total, context.getString(R.string.moeda_real)), cartId)
    }
}
