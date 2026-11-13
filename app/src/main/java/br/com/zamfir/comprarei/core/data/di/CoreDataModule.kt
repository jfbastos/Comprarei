package br.com.zamfir.comprarei.core.data.di

import br.com.zamfir.comprarei.core.data.auth.GoogleAuthClient
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.image.ImageCompressor
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.product.ProductRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.util.Constants
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreDataModule = module {
    single(named(Constants.IO_DISPATCHER)) { Dispatchers.IO }

    single { ProductRepository(androidContext(), get(), get(), get(named(Constants.IO_DISPATCHER))) }
    single { UserRepository(androidContext(), get(), get(), get(), get(named(Constants.IO_DISPATCHER))) }
    single { FirestoreRepository(androidContext(), get(), get(), get(named(Constants.IO_DISPATCHER))) }
    single { ConfigRepository(androidContext(), get(named(Constants.IO_DISPATCHER))) }
    single { ImageCompressor(androidContext(), get(named(Constants.IO_DISPATCHER))) }
    single { GoogleAuthClient() }
}
