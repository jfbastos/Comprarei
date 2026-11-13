package br.com.zamfir.comprarei.feature.category.data

import androidx.room.withTransaction
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.entity.Category
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Categorias. A tela de Categorias observa os Flows e não precisa recarregar depois de gravar.
 * O dispatcher só é usado nas funções que chamam o DAO de forma bloqueante.
 */
class CategoryRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    /** Leitura única, para o formulário validar nomes repetidos. */
    suspend fun getCategories() =  withContext(dispatcher) {appDatabase.CategoryDao().allCategories}

    fun observeCategories(): Flow<List<Category>> = appDatabase.CategoryDao().observeCategories()

    /** Quantidade de compras por id de categoria; categorias sem compras não aparecem no mapa. */
    fun observeCartCountByCategory(): Flow<Map<Int, Int>> =
        appDatabase.CartDao().observeCountByCategory().map { usage -> usage.associate { it.categoryId to it.cartCount } }

    suspend fun saveCategory(category: Category) = appDatabase.CategoryDao().insertCategory(category)

    /**
     * Exclui a categoria e deixa sem categoria as compras que a usavam, na mesma transação.
     * Sem isso as compras guardariam o id antigo, que o SQLite pode reaproveitar numa categoria nova.
     */
    suspend fun deleteCategory(category: Category) =
        appDatabase.withTransaction {
            appDatabase.CartDao().clearCategory(category.id)
            appDatabase.CategoryDao().deleteCategory(category)
        }

    suspend fun updateCategory(category: Category) = appDatabase.CategoryDao().updateCategory(category)

    suspend fun hasCartWithCategorie(id : Int) = withContext(dispatcher) { appDatabase.CartDao().countWithCategory(id) > 0 }

}
