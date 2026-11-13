package br.com.zamfir.comprarei.core.data.product

import android.content.Context
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.dao.CartDao
import br.com.zamfir.comprarei.core.database.dao.ProductDao
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.util.formatMoney
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Produtos de uma compra. Toda alteração que muda valores recalcula o total salvo na compra
 * (a Home mostra esse total), então as telas não precisam atualizá-lo por conta própria.
 */
class ProductRepository(
    private val context: Context,
    private val productDao: ProductDao,
    private val cartDao: CartDao,
    private val dispacher: CoroutineDispatcher,
) {

    suspend fun getProducts(cartId: Int) = withContext(dispacher){ productDao.getProducts(cartId) }

    suspend fun saveProduct(product: Product) : Long = withContext(dispacher) {
        productDao.insertProduct(product).also { updateCartTotal(product.cartId) }
    }

    suspend fun updateProduct(product: Product) = withContext(dispacher) {
        productDao.updateProduct(product)
        updateCartTotal(product.cartId)
    }

    suspend fun deleteProducts(cartId: Int) = withContext(dispacher) { productDao.deleteProductsFromCart(cartId) }

    suspend fun deleteProducts(products: List<Product>) = withContext(dispacher) {
        productDao.delete(products.map { it.id })
        products.map { it.cartId }.distinct().forEach { updateCartTotal(it) }
    }

    /** Desfaz uma exclusão: reinsere os produtos com os mesmos ids, posições e firestoreUUID. */
    suspend fun restoreProducts(products: List<Product>) = withContext(dispacher) {
        productDao.insertAll(products)
        products.map { it.cartId }.distinct().forEach { updateCartTotal(it) }
    }

    suspend fun updateDone(isDone: Boolean, id: Int) = withContext(dispacher) { productDao.updateDone(isDone, id) }

    suspend fun updateOrder(reorderedList: List<Product>) = withContext(dispacher) { productDao.updateOrder(reorderedList) }

    /*
     * O app calcula o total a partir dos produtos (ProductDao.progressByCart) e não lê este campo.
     * Ele continua sendo gravado só para os documentos do Firestore seguirem compatíveis com
     * versões antigas do app em outros aparelhos, que exibem esse texto.
     */
    private fun updateCartTotal(cartId: Int) {
        val total = productDao.getProducts(cartId).sumOf { it.price * it.quantity }
        cartDao.updateTotal(formatMoney(total, context.getString(R.string.moeda_real)), cartId)
    }
}
