package br.com.zamfir.comprarei.feature.profile.presentation

import br.com.zamfir.comprarei.feature.profile.presentation.deleteaccount.DeleteAccountViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profilePresentationModule = module {
    viewModelOf(::DeleteAccountViewModel)
    viewModelOf(::EditProfileViewModel)
}
