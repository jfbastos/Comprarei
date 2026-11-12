package br.com.zamfir.comprarei.core.database.dao

/** Resultado de [ProductDao.progressByCart]: quantos produtos a compra tem e quantos já foram comprados. */
data class CartProgress(
    val cartId: Int,
    val total: Int,
    val done: Int,
)
