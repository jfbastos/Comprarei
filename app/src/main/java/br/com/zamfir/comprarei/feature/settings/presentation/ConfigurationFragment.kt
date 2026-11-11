package br.com.zamfir.comprarei.feature.settings.presentation

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
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.feature.auth.presentation.LoginActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16). */
class ConfigurationFragment : Fragment() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ComprareiTheme {
                SettingsRoot(
                    viewModel = settingsViewModel,
                    onNavigateBack = { findNavController().popBackStack(R.id.homeFragment, false) },
                    onNavigateToProfile = { profilePicture ->
                        findNavController().navigate(R.id.action_configuracaoFragment_to_profileFragment, Bundle().apply {
                            putParcelable(getString(R.string.image_uri_key), profilePicture)
                        })
                    },
                    onNavigateToLogin = ::navigateToLogin,
                )
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        requireActivity().startActivity(intent)
        requireActivity().finish()
    }
}
