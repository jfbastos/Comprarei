package br.com.zamfir.comprarei.feature.settings.presentation

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val settingsPresentationModule = module {
    viewModelOf(::ConfigViewModel)
}
