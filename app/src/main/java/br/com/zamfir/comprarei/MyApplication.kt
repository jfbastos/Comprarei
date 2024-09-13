package br.com.zamfir.comprarei

import android.app.Application
import br.com.zamfir.comprarei.di.dataBaseModule
import br.com.zamfir.comprarei.di.dispatcherModule
import br.com.zamfir.comprarei.di.repositoryModule
import br.com.zamfir.comprarei.di.viewModelModule
import br.com.zamfir.comprarei.util.CrashHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

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