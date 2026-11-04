package br.com.zamfir.comprarei.app.di

import androidx.room.Room
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.feature.cart.data.CartRepository
import br.com.zamfir.comprarei.feature.category.data.CategoryRepository
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.product.ProductRepository
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.feature.cart.presentation.CartViewModel
import br.com.zamfir.comprarei.feature.category.presentation.CategoryViewModel
import br.com.zamfir.comprarei.feature.settings.presentation.ConfigViewModel
import br.com.zamfir.comprarei.feature.auth.presentation.LoginViewModel
import br.com.zamfir.comprarei.feature.product.presentation.ProductViewModel
import br.com.zamfir.comprarei.feature.profile.presentation.ProfileViewModel
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






