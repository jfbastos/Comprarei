package br.com.zamfir.comprarei.feature.category.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object CategoriesRoute : NavKey

fun EntryProviderScope<NavKey>.categoryEntries(onNavigateBack: () -> Unit) {
    entry<CategoriesRoute> {
        CategoriesRoot(
            viewModel = koinViewModel(),
            formViewModel = koinViewModel(),
            onNavigateBack = onNavigateBack,
        )
    }
}
