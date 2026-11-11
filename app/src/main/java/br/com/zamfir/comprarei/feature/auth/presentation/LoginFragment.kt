package br.com.zamfir.comprarei.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.app.MainActivity
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword.ForgotPasswordViewModel
import br.com.zamfir.comprarei.feature.auth.presentation.login.SignInRoot
import br.com.zamfir.comprarei.feature.auth.presentation.login.SignInViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16). */
class LoginFragment : Fragment() {

    private val signInViewModel: SignInViewModel by viewModel()
    private val forgotPasswordViewModel: ForgotPasswordViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComprareiTheme {
                    SignInRoot(
                        viewModel = signInViewModel,
                        forgotPasswordViewModel = forgotPasswordViewModel,
                        onNavigateToMain = {
                            startActivity(Intent(requireActivity(), MainActivity::class.java))
                            requireActivity().finish()
                        },
                        onNavigateToCreateAccount = {
                            findNavController().navigate(R.id.action_loginFragment_to_signinFragment)
                        },
                    )
                }
            }
        }
}
