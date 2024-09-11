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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentLoginBinding
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.isConectadoInternet
import br.com.zamfir.comprarei.util.isVisible
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
                loginViewModel.saveUserData(true)
            }

            override fun userCancelled() {
                binding.animationView.isVisible(false)
                binding.btnLoginGoogle.root.isVisible(true)
            }

            override fun loginError(exception: Exception?) {
                exception?.printStackTrace()
            }

        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setInfoFromLogonScreen()

        binding.createAccountLabel.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signinFragment)
        }

        binding.btnLogin.setOnClickListener {
            if(!requireContext().isConectadoInternet()){
                Snackbar.make(requireView(), getString(R.string.no_internet_connection), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(validateFields()){
                loginViewModel.loginWithEmail(
                    binding.user.text.toString(),
                    binding.password.text.toString()
                )
            }
        }

        binding.forgotPassword.setOnClickListener {
            if(!requireContext().isConectadoInternet()){
                Snackbar.make(requireView(), getString(R.string.no_internet_connection), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ForgotPasswordDialog().show(parentFragmentManager, "")
        }

        loginViewModel.localSaveState.observe(viewLifecycleOwner) {
            requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        }

        loginViewModel.loginState.observe(viewLifecycleOwner) { loginState ->
            showLoading(loginState.loading)
            if (loginState.loading) return@observe

            if (loginState.success && loginState.user != null) {
                requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
                return@observe
            }

            if (!loginState.success && loginState.error != null) {
                when(loginState.error){
                    is InvalidLogin -> {
                        showWarningInfo(isOnlyWarning = false, loginState.msgError ?: getString(R.string.wrong_user_or_password))
                    }
                }
            }
        }

        binding.btnLoginGoogle.root.setOnClickListener {
            if(!requireContext().isConectadoInternet()){
                Snackbar.make(requireView(), getString(R.string.no_internet_connection), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.animationView.isVisible(true)
            binding.btnLoginGoogle.root.isVisible(false)

            val snack = Snackbar.make(requireView(), "Trying to login with Google account...", Snackbar.LENGTH_INDEFINITE)

            val activity = (requireActivity() as LoginActivity)
            activity.googleSigninUtil.oneTapClient.beginSignIn( activity.googleSigninUtil.signUpRequest)
                .addOnSuccessListener(requireActivity()) { result ->
                    try {
                        activity.activityResult.launch(
                            IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        snack.setText(R.string.something_went_wrong)
                        snack.setAction(R.string.more){
                            AlertDialog.Builder(requireContext())
                                .setTitle(R.string.more_info)
                                .setMessage(getString(R.string.login_error_detail, e.localizedMessage))
                                .setPositiveButton(R.string.ok){ dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                        snack.show()
                    }
                }
                .addOnFailureListener(requireActivity()) { e ->
                    binding.animationView.isVisible(false)
                    binding.btnLoginGoogle.root.isVisible(true)
                    snack.setText(R.string.something_went_wrong)
                    snack.setAction(R.string.more){
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.more_info)
                            .setMessage(getString(R.string.login_error_detail, e.localizedMessage))
                            .setPositiveButton(R.string.ok){ dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    snack.show()
                }
        }
    }

    private fun showWarningInfo(isOnlyWarning : Boolean = true, msg : String) {
        binding.warningPlaceholder.isVisible(true)
        binding.warningMsg.text = msg
        if(!isOnlyWarning){
            binding.warningIcon.setImageResource(R.drawable.round_warning_24_red)
            binding.warningPlaceholder.background = ResourcesCompat.getDrawable(requireContext().resources, R.drawable.error_background, null)
            binding.warningMsg.setTextColor(requireActivity().resources.getColor(R.color.delete_red_text, null))
        }else{
            binding.warningIcon.setImageResource(R.drawable.round_warning_24_yellow)
            binding.warningPlaceholder.background = ResourcesCompat.getDrawable(requireContext().resources, R.drawable.warning_background, null)
            binding.warningMsg.setTextColor(requireActivity().resources.getColor(R.color.warning_yellow_text, null))
        }
    }

    private fun setInfoFromLogonScreen() {
        val login: String? = arguments?.getString(Constants.USER_KEY)
        val password: String? = arguments?.getString(Constants.PASSWORD_KEY)

        login?.let {
            binding.user.setText(it)
        }

        password?.let {
            binding.password.setText(it)
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
            showWarningInfo(true, getString(R.string.invalid_email))
            binding.userLayout.requestFocus()
            return false
        }

        if(binding.password.text.toString().length < 6){
            showWarningInfo(true, getString(R.string.msg_warning_invalid_password))
            return false
        }

        return true
    }

}