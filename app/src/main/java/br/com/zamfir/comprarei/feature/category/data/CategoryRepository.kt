package br.com.zamfir.comprarei.feature.category.data

import androidx.room.withTransaction
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.entity.Category
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CategoryRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    suspend fun getCategories() =  withContext(dispatcher) {appDatabase.CategoryDao().allCategories}

    /** Quantidade de compras por id de categoria; categorias sem compras não aparecem no mapa. */
    suspend fun getCartCountByCategory(): Map<Int, Int> = withContext(dispatcher) {
        appDatabase.CartDao().countByCategory().associate { it.categoryId to it.cartCount }
    }

    suspend fun saveCategory(category: Category) = withContext(dispatcher) { return@withContext appDatabase.CategoryDao().insertCategory(category)}

    /**
     * Exclui a categoria e deixa sem categoria as compras que a usavam, na mesma transação.
     * Sem isso as compras guardariam o id antigo, que o SQLite pode reaproveitar numa categoria nova.
     */
    suspend fun deleteCategory(category: Category) = withContext(dispatcher) {
        appDatabase.withTransaction {
            appDatabase.CartDao().clearCategory(category.id)
            appDatabase.CategoryDao().deleteCategory(category)
        }
    }

    suspend fun updateCategory(category: Category) = withContext(dispatcher) {appDatabase.CategoryDao().updateCategory(category)}

    suspend fun hasCartWithCategorie(id : Int) = withContext(dispatcher) { appDatabase.CartDao().countWithCategory(id) > 0 }

}
