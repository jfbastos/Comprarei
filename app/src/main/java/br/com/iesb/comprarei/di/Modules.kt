package br.com.iesb.comprarei.di

import androidx.room.Room
import br.com.iesb.comprarei.model.AppDatabase
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import br.com.iesb.comprarei.util.Constants
import br.com.iesb.comprarei.viewmodel.CartViewModel
import br.com.iesb.comprarei.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module


val dispatcherModule = module {
    single(named(Constants.IO_DISPATCHER)){ Dispatchers.IO }
}

val repositoryModule = module {
    single { CartRepository(get(), get(named(Constants.IO_DISPATCHER))) }
    single { ProductRepository(get(), get(named(Constants.IO_DISPATCHER))) }
}

val dataBaseModule = module {
    single { buildDatabase() }
    single { get<AppDatabase>().CartDao() }
    single { get<AppDatabase>().ProductDao() }
}

val viewModelModule = module {
    viewModel { CartViewModel(get(), get()) }
    viewModel { ProductViewModel(get())}
}

private fun Scope.buildDatabase() = Room.databaseBuilder(
    get(),
    AppDatabase::class.java,
    Constants.DATABASE_NAME
).fallbackToDestructiveMigration().build()






