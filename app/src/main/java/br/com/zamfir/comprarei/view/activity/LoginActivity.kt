package br.com.zamfir.comprarei.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.databinding.LoginActivityBinding

class LoginActivity : AppCompatActivity() {

    private var _binding : LoginActivityBinding? = null
    private val binding : LoginActivityBinding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}