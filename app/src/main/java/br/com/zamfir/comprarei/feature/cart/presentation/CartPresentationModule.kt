package br.com.zamfir.comprarei.feature.cart.presentation

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val cartPresentationModule = module {
    viewModelOf(::CartViewModel)
}
