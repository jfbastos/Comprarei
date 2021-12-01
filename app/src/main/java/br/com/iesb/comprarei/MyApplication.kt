package br.com.iesb.comprarei

import android.app.Application
import androidx.room.Room
import br.com.iesb.comprarei.model.AppDatabase

class MyApplication: Application() {

    companion object{
        var database : AppDatabase? = null
    }

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "comprarei_db")
            .fallbackToDestructiveMigration()
            .build()
    }
}