package br.com.zamfir.comprarei.feature.product.presentation

import br.com.zamfir.comprarei.feature.product.presentation.form.ProductFormViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val productPresentationModule = module {
    viewModel { params -> ProductsViewModel(cartId = params.get(), cartName = params.get(), productRepository = get(), configRepository = get()) }
    viewModelOf(::ProductFormViewModel)
}
