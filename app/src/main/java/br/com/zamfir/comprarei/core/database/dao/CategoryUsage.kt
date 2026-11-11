package br.com.zamfir.comprarei.core.database.dao

/** Resultado de [CartDao.countByCategory]. */
data class CategoryUsage(
    val categoryId: Int,
    val cartCount: Int,
)
