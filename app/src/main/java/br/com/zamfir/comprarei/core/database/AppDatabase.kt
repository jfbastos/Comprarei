package br.com.zamfir.comprarei.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.zamfir.comprarei.core.database.dao.CartDao
import br.com.zamfir.comprarei.core.database.dao.CategoryDao
import br.com.zamfir.comprarei.core.database.dao.ProductDao
import br.com.zamfir.comprarei.core.database.dao.UserInfoDao
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.database.entity.UserInfo

@Database(version = 1, entities = [Cart::class, Product::class, Category::class, UserInfo::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun CartDao() : CartDao
    abstract fun ProductDao() : ProductDao
    abstract fun CategoryDao() : CategoryDao
    abstract fun UserInfoDao() : UserInfoDao
}