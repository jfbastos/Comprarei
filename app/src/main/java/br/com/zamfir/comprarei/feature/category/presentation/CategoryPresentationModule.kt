package br.com.zamfir.comprarei.feature.category.presentation

import br.com.zamfir.comprarei.feature.category.presentation.form.CategoryFormViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val categoryPresentationModule = module {
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::CategoryFormViewModel)
}
