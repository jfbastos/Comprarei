package br.com.iesb.comprarei

import android.app.Application
import br.com.iesb.comprarei.di.dataBaseModule
import br.com.iesb.comprarei.di.dispatcherModule
import br.com.iesb.comprarei.di.repositoryModule
import br.com.iesb.comprarei.di.viewModelModule
import cat.ereza.customactivityoncrash.config.CaocConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CaocConfig.Builder.create()
            .enabled(true)
            .apply()

        startKoin {
            androidContext(applicationContext)
            modules(
                listOf(
                    dispatcherModule, dataBaseModule, repositoryModule,
                    viewModelModule
                )
            )
        }
    }
}