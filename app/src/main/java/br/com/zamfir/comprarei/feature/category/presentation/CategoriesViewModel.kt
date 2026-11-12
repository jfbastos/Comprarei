package br.com.zamfir.comprarei.feature.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import br.com.zamfir.comprarei.feature.category.data.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator

/** Categoria com a quantidade de compras que a usam. */
data class CategoryItem(val category: Category, val cartCount: Int)

data class CategoriesState(
    val isLoading: Boolean = true,
    val items: List<CategoryItem> = emptyList(),
    val isSearchActive: Boolean = false,
    val query: String = "",
    // Item aguardando confirmação de exclusão pela lixeira da lista.
    val pendingDelete: CategoryItem? = null,
) {
    val visibleItems: List<CategoryItem>
        get() {
            val typed = query.trim()
            if (!isSearchActive || typed.isEmpty()) return items
            return items.filter { it.category.description.contains(typed, ignoreCase = true) }
        }
}

sealed interface CategoriesAction {
    data object OnScreenShown : CategoriesAction
    data object OnBackClick : CategoriesAction
    data object OnSearchClick : CategoriesAction
    data class OnQueryChange(val query: String) : CategoriesAction
    data object OnCloseSearch : CategoriesAction
    data class OnDeleteClick(val item: CategoryItem) : CategoriesAction
    data object OnConfirmDelete : CategoriesAction
    data object OnCancelDelete : CategoriesAction

    // Resultados do formulário (sheet), que esta tela persiste.
    data class OnCategoryCreated(val category: Category) : CategoriesAction
    data class OnCategoryEdited(val category: Category) : CategoriesAction
    data class OnCategoryDeleted(val category: Category) : CategoriesAction
}

sealed interface CategoriesEvent {
    data object NavigateBack : CategoriesEvent

    data class ShowMessage(val message: UiText) : CategoriesEvent
}

class CategoriesViewModel(private val categoryRepository: CategoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    private val eventChannel = Channel<CategoriesEvent>()
    val events = eventChannel.receiveAsFlow()

    // Ordem alfabética que respeita acentos ("Água" junto de "Agua", não depois de "Z").
    private val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

    fun onAction(action: CategoriesAction) {
        when (action) {
            CategoriesAction.OnScreenShown -> load()
            CategoriesAction.OnBackClick -> {
                if (_state.value.isSearchActive) closeSearch() else sendEvent(CategoriesEvent.NavigateBack)
            }
            CategoriesAction.OnSearchClick -> _state.update { it.copy(isSearchActive = true) }
            is CategoriesAction.OnQueryChange -> _state.update { it.copy(query = action.query) }
            CategoriesAction.OnCloseSearch -> closeSearch()
            is CategoriesAction.OnDeleteClick -> _state.update { it.copy(pendingDelete = action.item) }
            CategoriesAction.OnConfirmDelete -> {
                val item = _state.value.pendingDelete ?: return
                _state.update { it.copy(pendingDelete = null) }
                delete(item.category)
            }
            CategoriesAction.OnCancelDelete -> _state.update { it.copy(pendingDelete = null) }
            is CategoriesAction.OnCategoryCreated -> persist { categoryRepository.saveCategory(action.category) }
            is CategoriesAction.OnCategoryEdited -> persist { categoryRepository.updateCategory(action.category) }
            is CategoriesAction.OnCategoryDeleted -> delete(action.category)
        }
    }

    private fun closeSearch() = _state.update { it.copy(isSearchActive = false, query = "") }

    private fun load() = viewModelScope.launch {
        try {
            val categories = categoryRepository.getCategories()
            val counts = categoryRepository.getCartCountByCategory()
            val items = categories
                .sortedWith { a, b -> collator.compare(a.description, b.description) }
                .map { CategoryItem(it, counts[it.id] ?: 0) }
            _state.update { it.copy(isLoading = false, items = items) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error("Falha ao carregar categorias", e)
            _state.update { it.copy(isLoading = false) }
            sendEvent(CategoriesEvent.ShowMessage(UiText.StringResource(R.string.categories_load_failed)))
        }
    }

    private fun delete(category: Category) = persist { categoryRepository.deleteCategory(category) }

    /**
     * Grava e recarrega a lista do banco. Recarregar (em vez de mexer na lista em memória)
     * garante ids corretos para categorias recém-criadas e contagens atualizadas.
     */
    private fun persist(operation: suspend () -> Any?) = viewModelScope.launch {
        try {
            operation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error("Falha ao salvar alteração de categoria", e)
            sendEvent(CategoriesEvent.ShowMessage(UiText.StringResource(R.string.categories_save_failed)))
        }
        load().join()
    }

    private fun sendEvent(event: CategoriesEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
