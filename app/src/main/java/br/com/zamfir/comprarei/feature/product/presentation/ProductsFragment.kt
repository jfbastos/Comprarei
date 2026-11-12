package br.com.zamfir.comprarei.feature.product.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.feature.product.presentation.form.ProductFormViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16).
 * Os argumentos do nav graph (cartId, cartName) chegam ao ProductsViewModel pelo SavedStateHandle.
 */
class ProductsFragment : Fragment() {

    private val productsViewModel: ProductsViewModel by viewModel()
    private val productFormViewModel: ProductFormViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComprareiTheme {
                    ProductsRoot(
                        viewModel = productsViewModel,
                        formViewModel = productFormViewModel,
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
}
