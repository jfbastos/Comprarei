package br.com.zamfir.comprarei.feature.auth.presentation

import br.com.zamfir.comprarei.feature.auth.presentation.createaccount.CreateAccountViewModel
import br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword.ForgotPasswordViewModel
import br.com.zamfir.comprarei.feature.auth.presentation.login.SignInViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::CreateAccountViewModel)
    viewModelOf(::SignInViewModel)
}
