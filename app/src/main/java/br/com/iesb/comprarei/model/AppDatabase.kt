package br.com.iesb.comprarei.model

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.iesb.comprarei.model.dao.CartDao
import br.com.iesb.comprarei.model.dao.ProductDao

@Database(entities = [Cart::class, Product::class], version = 19)
abstract class AppDatabase : RoomDatabase() {
    abstract fun CartDao() : CartDao
    abstract fun ProductDao() : ProductDao
}