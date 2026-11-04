package br.com.zamfir.comprarei.feature.profile.presentation

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val profilePresentationModule = module {
    viewModelOf(::ProfileViewModel)
}
