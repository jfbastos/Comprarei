package br.com.iesb.comprarei

import android.app.Application
import br.com.iesb.comprarei.di.dataBaseModule
import br.com.iesb.comprarei.di.dispachersModule
import br.com.iesb.comprarei.di.repositoryModule
import br.com.iesb.comprarei.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(applicationContext)
            modules(
                listOf(
                    dispachersModule, dataBaseModule, repositoryModule,
                    viewModelModule
                )
            )
        }
    }
}