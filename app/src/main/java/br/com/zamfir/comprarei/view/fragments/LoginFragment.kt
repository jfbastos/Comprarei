package br.com.zamfir.comprarei.view.fragments

import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentLoginBinding
import br.com.zamfir.comprarei.view.activity.LoginActivity
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

    private var _binding : FragmentLoginBinding? = null
    private val binding : FragmentLoginBinding get() = _binding!!

    private val loginViewModel : LoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val login : String? = arguments?.getString("USER_KEY")
        val password : String? = arguments?.getString("PASSWORD_KEY")

        login?.let {
            binding.user.setText(it)
        }

        password?.let {
            binding.password.setText(it)
        }

        binding.createAccountLabel.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signinFragment)
        }

        binding.btnLogin.setOnClickListener {
            loginViewModel.loginWithEmail(binding.user.text.toString(), binding.password.text.toString())
        }

        binding.btnLoginGoogle.root.setOnClickListener {
            val activity = (requireActivity() as LoginActivity)
            activity.oneTapClient.beginSignIn(activity.signUpRequest)
                .addOnSuccessListener(requireActivity()) { result ->
                    try {
                        activity.activityResult.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e("DEBUG", "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(requireActivity()) { e ->
                    // No Google Accounts found. Just continue presenting the signed-out UI.
                    Log.d("DEBUG", e.stackTraceToString())
                }
        }
    }

}