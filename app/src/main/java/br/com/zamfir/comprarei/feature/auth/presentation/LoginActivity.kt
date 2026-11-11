package br.com.zamfir.comprarei.feature.auth.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.databinding.LoginActivityBinding

/** Host do grafo de login até a unificação das Activities (3.16). */
class LoginActivity : AppCompatActivity() {

    private var _binding: LoginActivityBinding? = null
    private val binding: LoginActivityBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
