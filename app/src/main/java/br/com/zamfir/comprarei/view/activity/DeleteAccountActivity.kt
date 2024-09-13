package br.com.zamfir.comprarei.view.activity

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.ActivityDeleteAccountBinding
import br.com.zamfir.comprarei.util.GoogleSigninUtil
import br.com.zamfir.comprarei.util.exceptions.InvalidLogin
import br.com.zamfir.comprarei.util.isConectadoInternet
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.view.listeners.LoginWithGoogleListener
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import br.com.zamfir.comprarei.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeleteAccountActivity : AppCompatActivity() {

    private var _binding: ActivityDeleteAccountBinding? = null
    private val binding: ActivityDeleteAccountBinding get() = _binding!!

    private lateinit var activityResult: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var googleSigninUtil: GoogleSigninUtil

    private val loginViewModel: LoginViewModel by viewModel()
    private val profileViewModel: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityDeleteAccountBinding.inflate(layoutInflater)

        setContentView(binding.root)

        LoginWithGoogleListener.setOnListener(object : LoginWithGoogleListener {
            override fun userLoggedIn() {
                binding.msgLoggedWithGoogle.text = getString(R.string.successfully_re_authenticated)
                binding.animationView.isVisible(false)
                binding.btnLoginGoogle.root.isVisible(true)
            }

            override fun userCancelled() {
                binding.animationView.isVisible(false)
                binding.btnLoginGoogle.root.isVisible(true)
            }

            override fun loginError(exception: Exception?) {
                exception?.printStackTrace()
            }
        })

        loginViewModel.getUserInfo()

        activityResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            googleSigninUtil.doLogin(result)
        }

        loginViewModel.isLoginWithGoogle.observe(this){ isLoginWithGoogle ->
            binding.msgLoggedWithGoogle.isVisible(false)
            if(isLoginWithGoogle) {
                binding.passwordLayout.isEnabled = false
                binding.passwordLayout.alpha = 0.5f
                binding.emailLayout.isEnabled = false
                binding.emailLayout.alpha = 0.5f
                configLoginWithGoogle()
            }
        }

        profileViewModel.deleteUserException.observe(this){ exception ->
            if(exception != null) showWarningInfo(false, "Something went wrong on user account deletion.")
            else{
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }
        }

        loginViewModel.loginState.observe(this){ loginState ->
            if(loginState.loading) return@observe

            if(loginState.success) {
                binding.warningPlaceholder.isVisible(false)
                binding.loadingBtn.isVisible(false)
                binding.btnLogin.visibility = View.VISIBLE
                binding.btnLogin.text = getString(R.string.user_already_re_autenticated)
                binding.btnLogin.isEnabled = false
                return@observe
            }

            if (loginState.error != null) {
                binding.loadingBtn.isVisible(false)
                binding.btnLogin.visibility = View.VISIBLE

                when(loginState.error){
                    is InvalidLogin -> {
                        showWarningInfo(isOnlyWarning = false, loginState.msgError ?: getString(R.string.wrong_user_or_password))
                    }
                    else -> {
                        showWarningInfo(isOnlyWarning = false, loginState.msgError ?: getString(R.string.something_went_wrong))
                    }
                }
            }
        }

        binding.confirmButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Be careful")
                .setMessage("Do you confirm account deletion? All your info will be permanently deleted.")
                .setIcon(R.drawable.round_warning_24_yellow)
                .setPositiveButton(R.string.confirm){ dialog, _ ->
                    profileViewModel.deleteUser()
                    dialog.dismiss()
                }.show()
        }

        binding.btnLogin.setOnClickListener {
            if (!isConectadoInternet()) {
                showWarningInfo(true, getString(R.string.re_autentication_offline))
                return@setOnClickListener
            }


            if(validateFields()) {
                binding.btnLogin.visibility = View.INVISIBLE
                binding.loadingBtn.isVisible(true)
                loginViewModel.loginWithEmail(binding.email.text.toString(), binding.password.text.toString())
            }
        }
    }

    private fun configLoginWithGoogle() {
        googleSigninUtil = GoogleSigninUtil(this)

        binding.btnLoginGoogle.root.isVisible(true)
        binding.btnLoginGoogle.root.setOnClickListener {

            if (!isConectadoInternet()) {
                showWarningInfo(true, getString(R.string.no_connection_google_account))
                return@setOnClickListener
            }

            binding.animationView.isVisible(true)
            binding.btnLoginGoogle.root.isVisible(false)

            googleSigninUtil.oneTapClient.beginSignIn(googleSigninUtil.signUpRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        activityResult.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    } catch (e: IntentSender.SendIntentException) {
                        showWarningInfo(isOnlyWarning = false, getString(R.string.something_went_wrong_google_account))
                    }
                }
                .addOnFailureListener(this) { e ->
                    binding.animationView.isVisible(false)
                    binding.btnLoginGoogle.root.isVisible(true)
                    showWarningInfo(isOnlyWarning = false, getString(R.string.something_went_wrong_google_account))
                }
        }
    }

    private fun showWarningInfo(isOnlyWarning: Boolean = true, msg: String) {
        binding.warningPlaceholder.isVisible(true)
        binding.warningMsg.text = msg
        if (!isOnlyWarning) {
            binding.warningIcon.setImageResource(R.drawable.round_warning_24_red)
            binding.warningPlaceholder.background = ResourcesCompat.getDrawable(resources, R.drawable.error_background, null)
            binding.warningMsg.setTextColor(resources.getColor(R.color.delete_red_text, null))
        } else {
            binding.warningIcon.setImageResource(R.drawable.round_warning_24_yellow)
            binding.warningPlaceholder.background = ResourcesCompat.getDrawable(resources, R.drawable.warning_background, null)
            binding.warningMsg.setTextColor(resources.getColor(R.color.warning_yellow_text, null))
        }
    }

    private fun validateFields() : Boolean{
        val emailRegex = Regex("""^((?!\.)[\w\-_.]*[^.])(@\w+)(\.\w+(\.\w+)?[^.\W])$""")

        if(!emailRegex.matches(binding.email.text.toString())){
            showWarningInfo(true, getString(R.string.invalid_email))
            binding.emailLayout.requestFocus()
            return false
        }

        if(binding.password.text.toString().length < 6){
            showWarningInfo(true, getString(R.string.msg_warning_invalid_password))
            return false
        }

        return true
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}