package br.com.iesb.comprarei.viewmodel.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.viewmodel.CartViewModel

class CartViewModelFactory(private val cartRepository: CartRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CartViewModel(cartRepository) as T
    }
}