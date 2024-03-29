package br.com.zamfir.comprarei.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentSiginBinding

class SigninFragment : Fragment() {

    private var _binding : FragmentSiginBinding? = null
    private val binding : FragmentSiginBinding get() = _binding!!


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

            findNavController().navigate(R.id.action_signinFragment_to_loginFragment, arguments)
        }
    }

}