package br.com.zamfir.comprarei.core.database.di

import androidx.room.Room
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.migration.DatabaseMigrations
import br.com.zamfir.comprarei.core.util.Constants
import org.koin.dsl.module

val coreDatabaseModule = module {
    single {
        // Sem fallback destrutivo: se faltar uma migração, o app falha em vez de apagar o banco
        // (um banco vazio faria o próximo backup apagar os dados do usuário na nuvem).
        Room.databaseBuilder(get(), AppDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
    }
    single { get<AppDatabase>().CartDao() }
    single { get<AppDatabase>().ProductDao() }
    single { get<AppDatabase>().CategoryDao() }
    single { get<AppDatabase>().UserInfoDao() }
}
