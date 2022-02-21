package br.com.iesb.comprarei.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(applicationContext)
            modules(
                listOf(
                    dataBaseModule, repositoryModule,
                    viewModelModule
                )
            )
        }
    }
}