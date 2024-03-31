package br.com.zamfir.comprarei.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentSiginBinding
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.log

class SigninFragment : Fragment() {

    private var _binding : FragmentSiginBinding? = null
    private val binding : FragmentSiginBinding get() = _binding!!

    private val loginViewModel : LoginViewModel by viewModel()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSiginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            arguments = Bundle().apply {
                putString("USER_KEY", binding.user.text.toString())
                putString("PASSWORD_KEY", binding.passwordFirst.text.toString())
            }

            loginViewModel.createUser(binding.email.text.toString(), binding.user.text.toString(), binding.passwordFirst.text.toString())
        }
    }

}