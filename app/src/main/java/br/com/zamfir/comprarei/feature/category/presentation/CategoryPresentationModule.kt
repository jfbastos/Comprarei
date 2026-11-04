package br.com.zamfir.comprarei.feature.category.presentation

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val categoryPresentationModule = module {
    viewModelOf(::CategoryViewModel)
}
