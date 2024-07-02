package br.com.zamfir.comprarei.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.ActivityMainBinding
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            loginViewModel.hasLoggedUser()
        }

        loginViewModel.userLoggedState.observe(this) { isUserLogged ->
            if (!isUserLogged) {
                this.startActivity(Intent(this, LoginActivity::class.java))
                this.finish()
            } else {
                _binding = ActivityMainBinding.inflate(layoutInflater)

                setContentView(binding.root)

                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = resources.getColor(R.color.primary_green, null)
            }
        }
    }
}