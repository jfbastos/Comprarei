package br.com.zamfir.comprarei.app

import android.app.Application
import br.com.zamfir.comprarei.app.di.dataBaseModule
import br.com.zamfir.comprarei.app.di.dispatcherModule
import br.com.zamfir.comprarei.app.di.repositoryModule
import br.com.zamfir.comprarei.app.di.viewModelModule
import br.com.zamfir.comprarei.feature.crash.CrashHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CrashHandler.install(this)

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