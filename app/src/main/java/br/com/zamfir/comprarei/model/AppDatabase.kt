package br.com.zamfir.comprarei.model

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.zamfir.comprarei.model.dao.CartDao
import br.com.zamfir.comprarei.model.dao.CategoryDao
import br.com.zamfir.comprarei.model.dao.ProductDao
import br.com.zamfir.comprarei.model.dao.UserInfoDao
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.model.entity.UserInfo

@Database(version = 1, entities = [Cart::class, Product::class, Category::class, UserInfo::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun CartDao() : CartDao
    abstract fun ProductDao() : ProductDao
    abstract fun CategoryDao() : CategoryDao
    abstract fun UserInfoDao() : UserInfoDao
}