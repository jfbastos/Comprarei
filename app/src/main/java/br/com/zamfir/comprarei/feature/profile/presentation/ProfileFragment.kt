package br.com.zamfir.comprarei.feature.profile.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.feature.auth.presentation.DeleteAccountActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16). */
class ProfileFragment : Fragment() {

    private val editProfileViewModel: EditProfileViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Foto já carregada nas Configurações, para aparecer na hora enquanto o resto carrega.
        val initialPicture = arguments?.let { BundleCompat.getParcelable(it, getString(R.string.image_uri_key), Uri::class.java) }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComprareiTheme {
                    EditProfileRoot(
                        viewModel = editProfileViewModel,
                        initialPicture = initialPicture,
                        onNavigateBack = ::navigateBack,
                        onSaved = {
                            Toast.makeText(requireContext(), R.string.profile_successfully_edited, Toast.LENGTH_SHORT).show()
                            navigateBack()
                        },
                        onNavigateToDeleteAccount = {
                            startActivity(Intent(requireActivity(), DeleteAccountActivity::class.java))
                        },
                    )
                }
            }
        }
    }

    private fun navigateBack() {
        findNavController().popBackStack(R.id.configuracaoFragment, false)
    }
}
