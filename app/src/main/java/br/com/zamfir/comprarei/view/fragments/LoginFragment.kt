package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentLoginBinding
import br.com.zamfir.comprarei.util.errorAnimation
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.util.resetErrorAnimation
import br.com.zamfir.comprarei.view.activity.LoginActivity
import br.com.zamfir.comprarei.view.activity.MainActivity
import br.com.zamfir.comprarei.view.listeners.LoginWithGoogleListener
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding: FragmentLoginBinding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater)

        LoginWithGoogleListener.setOnListener(object : LoginWithGoogleListener {
            override fun userLoggedIn() {
                requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
            }

            override fun loginError(exception: Exception?) {
                exception?.printStackTrace()
            }

        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val login: String? = arguments?.getString("USER_KEY")
        val password: String? = arguments?.getString("PASSWORD_KEY")

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
            if(validateFields()){
                loginViewModel.loginWithEmail(
                    binding.user.text.toString(),
                    binding.password.text.toString()
                )
            }
        }

        binding.forgotPassword.setOnClickListener {
            ForgotPasswordDialog().show(parentFragmentManager, "")
        }

        loginViewModel.loginState.observe(viewLifecycleOwner) { loginState ->
            showLoading(loginState.loading)
            if (loginState.loading) return@observe

            if (loginState.success && loginState.user != null) {
                requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
                return@observe
            }

            if (!loginState.success && loginState.msgError.isNullOrBlank()) {
                Snackbar
                    .make(requireView(), R.string.something_went_wrong, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.more) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.more_info)
                            .setMessage(getString(R.string.login_error_detail, loginState.error))
                            .setPositiveButton(R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    .show()
            } else {
                Snackbar.make(requireView(), loginState.msgError.toString(), Snackbar.LENGTH_LONG)
                    .show()
            }
        }

        binding.btnLoginGoogle.root.setOnClickListener {
            val activity = (requireActivity() as LoginActivity)
            activity.oneTapClient.beginSignIn(activity.signUpRequest)
                .addOnSuccessListener(requireActivity()) { result ->
                    try {
                        activity.activityResult.launch(
                            IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        )
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

    private fun showLoading(loading: Boolean) {
        binding.loadingBtn.isVisible(loading)
        if(loading){
            binding.btnLogin.visibility = View.INVISIBLE
        }else{
            binding.btnLogin.visibility = View.VISIBLE
        }
    }

    private fun validateFields() : Boolean{
        val emailRegex = Regex("""^((?!\.)[\w\-_.]*[^.])(@\w+)(\.\w+(\.\w+)?[^.\W])$""")

        if(!emailRegex.matches(binding.user.text.toString())){
            binding.userLayout.errorAnimation(getString(R.string.invalid_email))
            binding.user.doOnTextChanged { _, _, _, _ ->
                binding.userLayout.resetErrorAnimation()
            }
            binding.userLayout.requestFocus()
            return false
        }

        if(binding.password.text.toString().length < 6){
            binding.passwordLayout.errorAnimation("Senha inválida", true)
            binding.password.doOnTextChanged { _, _, _, _ ->
                binding.passwordLayout.resetErrorAnimation()
            }
            binding.passwordLayout.requestFocus()
            return false
        }

        return true
    }

}