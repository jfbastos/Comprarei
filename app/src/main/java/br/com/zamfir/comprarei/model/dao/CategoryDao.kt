package br.com.zamfir.comprarei.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.zamfir.comprarei.model.entity.Category

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCategory(category: Category) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(categories : List<Category>)

    @get: Query("SELECT * FROM CATEGORIES")
    val allCategories : List<Category>

    @Query("SELECT * FROM CATEGORIES WHERE id = :id")
    fun getCategoryById(id : Int) : Category

    @Update
    fun updateCategory(category: Category)

    @Delete
    fun deleteCategory(category: Category)
}