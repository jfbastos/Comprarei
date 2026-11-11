package br.com.zamfir.comprarei.feature.auth.presentation

import br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword.ForgotPasswordViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ForgotPasswordViewModel)
}
