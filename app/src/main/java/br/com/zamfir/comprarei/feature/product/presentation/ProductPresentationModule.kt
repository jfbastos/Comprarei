package br.com.zamfir.comprarei.feature.product.presentation

import br.com.zamfir.comprarei.feature.product.presentation.form.ProductFormViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val productPresentationModule = module {
    viewModelOf(::ProductsViewModel)
    viewModelOf(::ProductFormViewModel)
}
