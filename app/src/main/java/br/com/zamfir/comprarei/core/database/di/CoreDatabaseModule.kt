package br.com.zamfir.comprarei.core.database.di

import androidx.room.Room
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.util.Constants
import org.koin.dsl.module

val coreDatabaseModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().CartDao() }
    single { get<AppDatabase>().ProductDao() }
    single { get<AppDatabase>().CategoryDao() }
    single { get<AppDatabase>().UserInfoDao() }
}
