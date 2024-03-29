package br.com.zamfir.comprarei.view.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import br.com.zamfir.comprarei.databinding.ActivityMainBinding
import br.com.zamfir.comprarei.util.Constants

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setDarkMode()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun saveShared(modeNightMode: Int){
        val sharedPreferences = this.getPreferences(Context.MODE_PRIVATE)
        with (sharedPreferences.edit()){
            putInt(Constants.DARK_MODE, modeNightMode)
            apply()
        }
    }

    private fun setDarkMode() {
        val sharedPreferences = this.getPreferences(Context.MODE_PRIVATE)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            AppCompatDelegate.setDefaultNightMode(sharedPreferences.getInt(Constants.DARK_MODE, -1))
        }else{
            AppCompatDelegate.setDefaultNightMode(sharedPreferences.getInt(Constants.DARK_MODE, 0))
        }

    }

}