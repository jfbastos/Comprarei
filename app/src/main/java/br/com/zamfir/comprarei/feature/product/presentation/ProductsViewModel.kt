package br.com.zamfir.comprarei.feature.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.product.ProductRepository
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsState(
    val cartName: String = "",
    val isLoading: Boolean = true,
    // Na ordem salva (posição).
    val products: List<Product> = emptyList(),
    val moveDoneToBottom: Boolean = false,
    val sort: ProductSort = ProductSort.Manual,
    val isSearchActive: Boolean = false,
    val query: String = "",
    // firestoreUUID dos selecionados; não vazio = modo de seleção.
    val selectedIds: Set<String> = emptySet(),
) {
    val visibleProducts: List<Product> = products.applySortAndSearch(sort, query, moveDoneToBottom)

    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()

    // Arrastar só faz sentido vendo a ordem salva completa.
    val canReorder: Boolean get() = sort == ProductSort.Manual && query.isBlank() && !isSelectionMode

    val total: Double = products.sumOf { it.subtotal }
    val doneCount: Int = products.count { it.done }
    val remaining: Double = products.filterNot { it.done }.sumOf { it.subtotal }
}

sealed interface ProductsAction {
    data object OnBackClick : ProductsAction
    data class OnProductClick(val product: Product) : ProductsAction
    data class OnProductLongClick(val product: Product) : ProductsAction
    data class OnToggleDone(val product: Product) : ProductsAction
    data class OnDelete(val products: List<Product>) : ProductsAction
    data class OnUndoDelete(val products: List<Product>) : ProductsAction
    data class OnMove(val fromKey: Any, val toKey: Any) : ProductsAction
    data object OnMoveFinished : ProductsAction
    data object OnAddClick : ProductsAction
    data class OnProductCreated(val product: Product) : ProductsAction
    data class OnProductEdited(val product: Product) : ProductsAction
    data object OnShareClick : ProductsAction

    data object OnSearchClick : ProductsAction
    data class OnQueryChange(val value: String) : ProductsAction
    data object OnCloseSearch : ProductsAction
    data class OnSortChange(val sort: ProductSort) : ProductsAction

    data object OnSelectAllClick : ProductsAction
    data object OnDeleteSelectedClick : ProductsAction
    data object OnClearSelection : ProductsAction
}

sealed interface ProductsEvent {
    data object NavigateBack : ProductsEvent
    data class OpenNewProduct(val cartId: Int, val position: Int) : ProductsEvent
    data class OpenEditProduct(val product: Product) : ProductsEvent

    /** Exclusão feita; a tela oferece desfazer com [products]. */
    data class ProductsDeleted(val products: List<Product>) : ProductsEvent
    data class Share(val cartName: String, val products: List<Product>) : ProductsEvent
    data class ShowMessage(val message: UiText) : ProductsEvent
}

/** [cartId] e [cartName] vêm da rota (ProductsRoute) via parâmetros do Koin. */
class ProductsViewModel(
    private val cartId: Int,
    cartName: String,
    private val productRepository: ProductRepository,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductsState(cartName = cartName))
    val state: StateFlow<ProductsState> = _state.asStateFlow()

    private val eventChannel = Channel<ProductsEvent>()
    val events = eventChannel.receiveAsFlow()

    // Última lista vinda do banco. Mudanças otimistas (marcar, excluir, arrastar) voltam a ela se a gravação falhar.
    private var savedProducts: List<Product> = emptyList()

    // Durante o arrasto a tela mostra a ordem em andamento; uma emissão do banco no meio desfaria o movimento.
    private var isReordering = false

    init {
        viewModelScope.launch {
            // A preferência vem antes da lista, para ela não aparecer numa ordem e logo trocar.
            val moveDoneToBottom = configRepository.getIsToMoveToBottomDoneItens()
            _state.update { it.copy(moveDoneToBottom = moveDoneToBottom) }

            productRepository.observeProducts(cartId)
                .catch { e ->
                    AppLogger.error("Falha ao carregar os produtos", e)
                    _state.update { it.copy(isLoading = false) }
                    sendEvent(ProductsEvent.ShowMessage(UiText.StringResource(R.string.products_load_error)))
                }
                .collect { products ->
                    savedProducts = products
                    if (!isReordering) showProducts(products)
                }
        }
    }

    fun onAction(action: ProductsAction) {
        when (action) {
            ProductsAction.OnBackClick -> onBack()
            is ProductsAction.OnProductClick -> {
                if (_state.value.isSelectionMode) toggleSelection(action.product)
                else sendEvent(ProductsEvent.OpenEditProduct(action.product))
            }
            is ProductsAction.OnProductLongClick -> toggleSelection(action.product)
            is ProductsAction.OnToggleDone -> toggleDone(action.product)
            is ProductsAction.OnDelete -> delete(action.products)
            is ProductsAction.OnUndoDelete -> undoDelete(action.products)
            is ProductsAction.OnMove -> move(action.fromKey, action.toKey)
            ProductsAction.OnMoveFinished -> saveOrder()
            ProductsAction.OnAddClick -> sendEvent(ProductsEvent.OpenNewProduct(cartId, _state.value.products.size))
            // O formulário só devolve o produto; quem salva é a tela. A lista se atualiza pelo Flow.
            is ProductsAction.OnProductCreated -> launchSafely("Falha ao salvar o produto") {
                productRepository.saveProduct(action.product)
            }
            is ProductsAction.OnProductEdited -> launchSafely("Falha ao editar o produto") {
                productRepository.updateProduct(action.product)
            }
            ProductsAction.OnShareClick -> _state.value.let { sendEvent(ProductsEvent.Share(it.cartName, it.products)) }

            ProductsAction.OnSearchClick -> _state.update { it.copy(isSearchActive = true) }
            is ProductsAction.OnQueryChange -> _state.update { it.copy(query = action.value) }
            ProductsAction.OnCloseSearch -> _state.update { it.copy(isSearchActive = false, query = "") }
            is ProductsAction.OnSortChange -> _state.update { it.copy(sort = action.sort) }

            ProductsAction.OnSelectAllClick -> _state.update { current ->
                val visibleIds = current.visibleProducts.map { it.firestoreUUID }.toSet()
                // Tudo já selecionado: o mesmo botão limpa a seleção.
                current.copy(selectedIds = if (current.selectedIds.containsAll(visibleIds)) emptySet() else visibleIds)
            }
            ProductsAction.OnDeleteSelectedClick -> _state.value.let { current ->
                delete(current.products.filter { it.firestoreUUID in current.selectedIds })
            }
            ProductsAction.OnClearSelection -> _state.update { it.copy(selectedIds = emptySet()) }
        }
    }

    // Voltar fecha primeiro a seleção, depois a busca, e só então sai da tela.
    private fun onBack() {
        val current = _state.value
        when {
            current.isSelectionMode -> _state.update { it.copy(selectedIds = emptySet()) }
            current.isSearchActive -> _state.update { it.copy(isSearchActive = false, query = "") }
            else -> sendEvent(ProductsEvent.NavigateBack)
        }
    }

    private fun showProducts(products: List<Product>) {
        _state.update { current ->
            val ids = products.map { it.firestoreUUID }.toSet()
            current.copy(isLoading = false, products = products, selectedIds = current.selectedIds intersect ids)
        }
    }

    private fun toggleSelection(product: Product) {
        _state.update {
            val id = product.firestoreUUID
            it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
        }
    }

    // Atualiza a tela na hora (o check não espera o banco); a emissão do Flow confirma em seguida.
    private fun toggleDone(product: Product) {
        val done = !product.done
        _state.update { current ->
            current.copy(products = current.products.map { if (it.firestoreUUID == product.firestoreUUID) it.copy(done = done) else it })
        }
        launchSafely("Falha ao marcar o produto") { productRepository.updateDone(done, product.id) }
    }

    // Some da lista na hora, para o gesto de deslizar não voltar o item antes do banco responder.
    private fun delete(products: List<Product>) {
        if (products.isEmpty()) return
        val ids = products.map { it.firestoreUUID }.toSet()
        _state.update { current ->
            current.copy(products = current.products.filterNot { it.firestoreUUID in ids }, selectedIds = emptySet())
        }
        launchSafely("Falha ao excluir produtos") {
            productRepository.deleteProducts(products)
            sendEvent(ProductsEvent.ProductsDeleted(products))
        }
    }

    private fun undoDelete(products: List<Product>) {
        launchSafely("Falha ao desfazer a exclusão") { productRepository.restoreProducts(products) }
    }

    // Move na lista exibida (ordem manual, talvez com comprados no fim) e passa a usá-la como ordem salva.
    private fun move(fromKey: Any, toKey: Any) {
        if (!_state.value.canReorder) return
        isReordering = true
        _state.update { current ->
            val list = current.visibleProducts.toMutableList()
            val from = list.indexOfFirst { it.firestoreUUID == fromKey }
            val to = list.indexOfFirst { it.firestoreUUID == toKey }
            if (from < 0 || to < 0) return@update current
            list.add(to, list.removeAt(from))
            current.copy(products = list)
        }
    }

    private fun saveOrder() {
        if (!isReordering) return
        val reordered = _state.value.products.mapIndexed { index, product -> product.copy(position = index) }
        _state.update { it.copy(products = reordered) }
        launchSafely("Falha ao salvar a ordem dos produtos") {
            try {
                productRepository.updateOrder(reordered)
            } finally {
                isReordering = false
            }
        }
    }

    private fun launchSafely(logMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.error(logMessage, e)
                sendEvent(ProductsEvent.ShowMessage(UiText.StringResource(R.string.save_change_error)))
                // Desfaz a mudança otimista: volta ao que está no banco.
                showProducts(savedProducts)
            }
        }
    }

    private fun sendEvent(event: ProductsEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
