package br.com.zamfir.comprarei.feature.cart.presentation

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cartPresentationModule = module {
    viewModelOf(::CartViewModel)
}
