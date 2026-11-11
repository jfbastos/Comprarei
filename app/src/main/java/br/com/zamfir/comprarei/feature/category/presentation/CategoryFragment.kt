package br.com.zamfir.comprarei.feature.category.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.listeners.InfoUpdateListener
import br.com.zamfir.comprarei.feature.category.presentation.form.CategoryFormViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16). */
class CategoryFragment : Fragment() {

    private val categoriesViewModel: CategoriesViewModel by viewModel()
    private val categoryFormViewModel: CategoryFormViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComprareiTheme {
                    CategoriesRoot(
                        viewModel = categoriesViewModel,
                        formViewModel = categoryFormViewModel,
                        onNavigateBack = { findNavController().navigateUp() },
                        // A Home escuta este listener para recarregar as compras (some na 3.15).
                        onCategoriesChanged = { InfoUpdateListener.infoUpdateListener.infoUpdated() },
                    )
                }
            }
        }
}
