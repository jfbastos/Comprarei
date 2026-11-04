package br.com.zamfir.comprarei.feature.cart.data

import br.com.zamfir.comprarei.core.util.Constants
import org.koin.core.qualifier.named
import org.koin.dsl.module

val cartDataModule = module {
    single { CartRepository(get(), get(named(Constants.IO_DISPATCHER))) }
}
