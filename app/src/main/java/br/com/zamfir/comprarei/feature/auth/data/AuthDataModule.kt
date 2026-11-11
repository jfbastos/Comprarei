package br.com.zamfir.comprarei.feature.auth.data

import org.koin.dsl.module

val authDataModule = module {
    single { GoogleAuthClient() }
}
