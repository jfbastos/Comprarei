package br.com.iesb.comprarei.di

import androidx.room.Room
import br.com.iesb.comprarei.model.AppDatabase
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import br.com.iesb.comprarei.viewmodel.CartViewModel
import br.com.iesb.comprarei.viewmodel.ProductViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named


val dispachersModule = module {
    single(named("IODispacher")){
        Dispatchers.IO
    }
}

val repositoryModule = module {
    single {
        CartRepository(get(), get(named("IODispacher")))
    }

    single {
        ProductRepository(get(), get(named("IODispacher")))
    }
}

val dataBaseModule = module {
    single {
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "comprarei_db"
        ).fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().CartDao()}
    single { get<AppDatabase>().ProductDao()}
}

val viewModelModule = module {
    viewModel { CartViewModel(get(), get()) }
    viewModel { ProductViewModel(get())}
}






