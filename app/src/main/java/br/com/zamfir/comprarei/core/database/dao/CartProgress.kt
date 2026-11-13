package br.com.zamfir.comprarei.core.database.dao

/**
 * Resultado de [ProductDao.observeProgressByCart]: quantos produtos a compra tem, quantos já foram
 * comprados e o valor total (soma de preço × quantidade).
 */
data class CartProgress(
    val cartId: Int,
    val total: Int,
    val done: Int,
    val totalValue: Double = 0.0,
)
