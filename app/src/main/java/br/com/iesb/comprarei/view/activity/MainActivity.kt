package br.com.iesb.comprarei.view.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import br.com.iesb.comprarei.databinding.ActivityMainBinding
import android.content.Intent
import android.view.KeyEvent
import android.widget.Toast

import androidx.core.app.NavUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            CoroutineScope(Dispatchers.IO).launch{
                delay(3000)
            }
        }
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (this as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}