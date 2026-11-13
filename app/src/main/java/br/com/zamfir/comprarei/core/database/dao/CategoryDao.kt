package br.com.zamfir.comprarei.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.zamfir.comprarei.core.database.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: Category) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(categories : List<Category>)

    @get: Query("SELECT * FROM CATEGORIES")
    val allCategories : List<Category>

    @Query("SELECT * FROM CATEGORIES")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM CATEGORIES WHERE id = :id")
    fun getCategoryById(id : Int) : Category?

    @Query("DELETE FROM CATEGORIES")
    fun deleteAll()

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}