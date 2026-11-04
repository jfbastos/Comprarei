package br.com.zamfir.comprarei.feature.product.presentation

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val productPresentationModule = module {
    viewModelOf(::ProductViewModel)
}
