package br.com.zamfir.comprarei.data.repositories

import br.com.zamfir.comprarei.data.model.AppDatabase
import br.com.zamfir.comprarei.data.model.entity.Category
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class CategoryRepository(private val appDatabase: AppDatabase, private val dispatcher: CoroutineDispatcher) {

    suspend fun getCategories() =  withContext(dispatcher) {appDatabase.CategoryDao().allCategories}

    suspend fun saveCategory(category: Category) = withContext(dispatcher) { return@withContext appDatabase.CategoryDao().insertCategory(category)}

    suspend fun deleteCategory(category: Category) = withContext(dispatcher) {appDatabase.CategoryDao().deleteCategory(category)}

    suspend fun updateCategory(category: Category) = withContext(dispatcher) {appDatabase.CategoryDao().updateCategory(category)}

    suspend fun hasCartWithCategorie(id : Int) = withContext(dispatcher) {appDatabase.CartDao().carts.find { it.categoryId == id } != null}

}