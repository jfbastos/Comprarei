package br.com.zamfir.comprarei.core.database.dao

/** Resultado de [CartDao.observeCountByCategory]. */
data class CategoryUsage(
    val categoryId: Int,
    val cartCount: Int,
)
