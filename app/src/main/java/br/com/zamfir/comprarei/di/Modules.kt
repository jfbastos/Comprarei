package br.com.zamfir.comprarei.di

import androidx.room.Room
import br.com.zamfir.comprarei.model.AppDatabase
import br.com.zamfir.comprarei.repositories.CartRepository
import br.com.zamfir.comprarei.repositories.CategoryRepository
import br.com.zamfir.comprarei.repositories.ConfigRepository
import br.com.zamfir.comprarei.repositories.UserRepository
import br.com.zamfir.comprarei.repositories.FirestoreRepository
import br.com.zamfir.comprarei.repositories.ProductRepository
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.viewmodel.CartViewModel
import br.com.zamfir.comprarei.viewmodel.CategoryViewModel
import br.com.zamfir.comprarei.viewmodel.ConfigViewModel
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import br.com.zamfir.comprarei.viewmodel.ProductViewModel
import br.com.zamfir.comprarei.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module


val dispatcherModule = module {
    single(named(Constants.IO_DISPATCHER)) { Dispatchers.IO }
}

val repositoryModule = module {
    single { CartRepository(get(), get(named(Constants.IO_DISPATCHER))) }
    single { ProductRepository(get(), get(named(Constants.IO_DISPATCHER))) }
    single { CategoryRepository(get(), get(named(Constants.IO_DISPATCHER))) }
    single { UserRepository(androidContext() ,get(), get(named(Constants.IO_DISPATCHER))) }
    single { FirestoreRepository(androidContext() ,get(), get(named(Constants.IO_DISPATCHER))) }
    single { ConfigRepository(androidContext(), get(named(Constants.IO_DISPATCHER))) }
}

val dataBaseModule = module {
    single { buildDatabase() }
    single { get<AppDatabase>().CartDao() }
    single { get<AppDatabase>().ProductDao() }
    single { get<AppDatabase>().CategoryDao() }
    single { get<AppDatabase>().UserInfoDao() }
}

val viewModelModule = module {
    viewModel { CartViewModel(get(), get(), get(), get()) }
    viewModel { ProductViewModel(get(), get()) }
    viewModel { CategoryViewModel(get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { ConfigViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get()) }
}

private fun Scope.buildDatabase() = Room.databaseBuilder(
    get(),
    AppDatabase::class.java,
    Constants.DATABASE_NAME
).fallbackToDestructiveMigration().build()






