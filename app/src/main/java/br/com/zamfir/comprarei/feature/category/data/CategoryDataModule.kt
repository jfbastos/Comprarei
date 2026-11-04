package br.com.zamfir.comprarei.feature.category.data

import br.com.zamfir.comprarei.core.util.Constants
import org.koin.core.qualifier.named
import org.koin.dsl.module

val categoryDataModule = module {
    single { CategoryRepository(get(), get(named(Constants.IO_DISPATCHER))) }
}
