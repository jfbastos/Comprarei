package br.com.zamfir.comprarei.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.app.MainActivity
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.feature.auth.presentation.createaccount.CreateAccountRoot
import br.com.zamfir.comprarei.feature.auth.presentation.createaccount.CreateAccountViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16). */
class CreateAccountFragment : Fragment() {

    private val createAccountViewModel: CreateAccountViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComprareiTheme {
                    CreateAccountRoot(
                        viewModel = createAccountViewModel,
                        onNavigateBack = { findNavController().navigateUp() },
                        onAccountCreated = { photoFailed ->
                            if (photoFailed) {
                                Toast.makeText(requireContext(), R.string.account_created_without_photo, Toast.LENGTH_LONG).show()
                            }
                            startActivity(Intent(requireActivity(), MainActivity::class.java))
                            requireActivity().finish()
                        },
                    )
                }
            }
        }
}
