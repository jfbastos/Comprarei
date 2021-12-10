package br.com.iesb.comprarei.viewmodel.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.model.ProductRepository
import br.com.iesb.comprarei.viewmodel.ProductViewModel

class ProductViewModelFactory(val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProductViewModel(repository) as T
    }
}