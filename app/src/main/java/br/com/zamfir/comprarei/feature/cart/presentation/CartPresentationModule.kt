package br.com.zamfir.comprarei.feature.cart.presentation

import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormViewModel
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cartPresentationModule = module {
    viewModelOf(::CartsViewModel)
    viewModelOf(::CartFormViewModel)
    viewModelOf(::CartFilterViewModel)
}
