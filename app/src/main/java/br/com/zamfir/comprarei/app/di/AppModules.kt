package br.com.zamfir.comprarei.app.di

import br.com.zamfir.comprarei.app.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import br.com.zamfir.comprarei.core.data.di.coreDataModule
import br.com.zamfir.comprarei.core.database.di.coreDatabaseModule
import br.com.zamfir.comprarei.feature.auth.presentation.authPresentationModule
import br.com.zamfir.comprarei.feature.cart.data.cartDataModule
import br.com.zamfir.comprarei.feature.cart.presentation.cartPresentationModule
import br.com.zamfir.comprarei.feature.category.data.categoryDataModule
import br.com.zamfir.comprarei.feature.category.presentation.categoryPresentationModule
import br.com.zamfir.comprarei.feature.product.presentation.productPresentationModule
import br.com.zamfir.comprarei.feature.profile.presentation.profilePresentationModule
import br.com.zamfir.comprarei.feature.settings.presentation.settingsPresentationModule

/** ViewModel da MainActivity (estado de login e tema). */
val appModule = module {
    viewModelOf(::MainViewModel)
}

val appModules = listOf(
    appModule,
    // core
    coreDatabaseModule,
    coreDataModule,
    // features
    cartDataModule,
    cartPresentationModule,
    productPresentationModule,
    categoryDataModule,
    categoryPresentationModule,
    authPresentationModule,
    profilePresentationModule,
    settingsPresentationModule,
)
