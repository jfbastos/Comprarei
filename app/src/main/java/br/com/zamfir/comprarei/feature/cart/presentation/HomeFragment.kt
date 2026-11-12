package br.com.zamfir.comprarei.feature.cart.presentation

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
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterViewModel
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a Home em Compose até os hosts migrarem para Navigation 3 (3.16). */
class HomeFragment : Fragment() {

    private val cartsViewModel: CartsViewModel by viewModel()
    private val cartFormViewModel: CartFormViewModel by viewModel()
    private val cartFilterViewModel: CartFilterViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComprareiTheme {
                    CartsRoot(
                        viewModel = cartsViewModel,
                        formViewModel = cartFormViewModel,
                        filterViewModel = cartFilterViewModel,
                        onOpenCart = { cart ->
                            val args = Bundle().apply {
                                putString(Constants.CART_NAME_KEY, cart.name)
                                putLong(Constants.CART_ID_KEY, cart.id.toLong())
                            }
                            findNavController().navigate(R.id.action_homeFragment_to_productsFragment, args)
                        },
                        onOpenCategories = { findNavController().navigate(R.id.action_homeFragment_to_categoryFragment) },
                        onOpenSettings = { findNavController().navigate(R.id.action_homeFragment_to_configuracaoFragment) },
                    )
                }
            }
        }
}
