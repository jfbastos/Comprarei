package br.com.zamfir.comprarei.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.database.dao.CartProgress
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.database.entity.UserInfo
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.log.AppLogger
import br.com.zamfir.comprarei.feature.cart.data.CartRepository
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilter
import br.com.zamfir.comprarei.feature.cart.presentation.filter.applyFilter
import br.com.zamfir.comprarei.core.util.toCents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartsState(
    val isLoading: Boolean = true,
    // Na ordem salva (posição), com a categoria já preenchida.
    val carts: List<Cart> = emptyList(),
    // Por id local da compra; compra sem produtos não tem entrada.
    val progress: Map<Int, CartProgress> = emptyMap(),
    val user: UserInfo? = null,
    val showTotal: Boolean = true,
    val filter: CartFilter = CartFilter(),
    val isSearchActive: Boolean = false,
    val query: String = "",
    // firestoreUUID das selecionadas; não vazio = modo de seleção.
    val selectedIds: Set<String> = emptySet(),
    val isCompletedExpanded: Boolean = true,
) {
    // Valor de cada compra em centavos, calculado a partir dos produtos (compra sem produtos = 0).
    val totalCents: Map<Int, Long> = progress.mapValues { it.value.totalValue.toCents() }

    val visibleCarts: List<Cart> = carts.applyFilter(filter, totalCents).search(query)

    // Seções da lista, mantendo a ordem (manual ou do filtro) dentro de cada uma.
    val ongoingCarts: List<Cart> = visibleCarts.filterNot { isCompleted(it) }
    val completedCarts: List<Cart> = visibleCarts.filter { isCompleted(it) }

    // Resumo do cabeçalho: sempre sobre todas as compras, sem filtro nem busca.
    val openCount: Int = carts.count { !isCompleted(it) }

    /** Concluída = tem produtos e todos foram comprados. Compra vazia continua em andamento. */
    fun isCompleted(cart: Cart): Boolean = progress[cart.id]?.let { it.total > 0 && it.done == it.total } ?: false

    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()

    // Arrastar só faz sentido vendo a lista completa na ordem salva.
    val canReorder: Boolean get() = !filter.isActive && query.isBlank() && !isSelectionMode
}

private fun List<Cart>.search(query: String): List<Cart> {
    val term = query.trim()
    if (term.isEmpty()) return this
    return filter { it.name.contains(term, ignoreCase = true) || it.data.contains(term) }
}

sealed interface CartsAction {
    /** A tela voltou a aparecer: produtos, categorias e configurações podem ter mudado. */
    data object OnResume : CartsAction
    data object OnBackClick : CartsAction
    data class OnCartClick(val cart: Cart) : CartsAction
    data class OnCartLongClick(val cart: Cart) : CartsAction
    data class OnEditClick(val cart: Cart) : CartsAction
    data class OnDelete(val carts: List<Cart>) : CartsAction
    data class OnUndoDelete(val carts: List<Cart>, val products: List<Product>) : CartsAction
    data class OnMove(val fromKey: Any, val toKey: Any) : CartsAction
    data object OnMoveFinished : CartsAction
    data object OnAddClick : CartsAction
    data class OnCartCreated(val cart: Cart) : CartsAction
    data class OnCartEdited(val cart: Cart) : CartsAction

    data object OnSearchClick : CartsAction
    data class OnQueryChange(val value: String) : CartsAction
    data object OnCloseSearch : CartsAction
    data object OnFilterClick : CartsAction
    data class OnFilterApplied(val filter: CartFilter) : CartsAction
    data object OnClearFilter : CartsAction
    data object OnToggleCompletedSection : CartsAction

    data object OnSelectAllClick : CartsAction
    data object OnDeleteSelectedClick : CartsAction
    data object OnClearSelection : CartsAction
}

sealed interface CartsEvent {
    data class OpenCart(val cart: Cart) : CartsEvent
    data class OpenNewCart(val position: Int) : CartsEvent
    data class OpenEditCart(val cart: Cart) : CartsEvent
    data class OpenFilter(val filter: CartFilter, val carts: List<Cart>, val totalCents: Map<Int, Long>) : CartsEvent

    /** Exclusão feita; a tela oferece desfazer com as compras e os produtos delas. */
    data class CartsDeleted(val carts: List<Cart>, val products: List<Product>) : CartsEvent
    data class ShowMessage(val message: UiText) : CartsEvent
}

class CartsViewModel(
    private val cartRepository: CartRepository,
    private val configRepository: ConfigRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CartsState())
    val state: StateFlow<CartsState> = _state.asStateFlow()

    private val eventChannel = Channel<CartsEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: CartsAction) {
        when (action) {
            CartsAction.OnResume -> launchSafely("Falha ao carregar as compras") { reload() }
            CartsAction.OnBackClick -> onBack()
            is CartsAction.OnCartClick -> {
                if (_state.value.isSelectionMode) toggleSelection(action.cart)
                else sendEvent(CartsEvent.OpenCart(action.cart))
            }
            is CartsAction.OnCartLongClick -> toggleSelection(action.cart)
            is CartsAction.OnEditClick -> sendEvent(CartsEvent.OpenEditCart(action.cart))
            is CartsAction.OnDelete -> delete(action.carts)
            is CartsAction.OnUndoDelete -> launchSafely("Falha ao desfazer a exclusão de compras") {
                cartRepository.restoreCarts(action.carts, action.products)
                reload()
            }
            is CartsAction.OnMove -> move(action.fromKey, action.toKey)
            CartsAction.OnMoveFinished -> saveOrder()
            CartsAction.OnAddClick -> {
                val position = _state.value.carts.maxOfOrNull { it.position }?.plus(1) ?: 0
                sendEvent(CartsEvent.OpenNewCart(position))
            }
            // O formulário só devolve a compra; salvar e recarregar (para ter o id local) fica aqui.
            is CartsAction.OnCartCreated -> launchSafely("Falha ao salvar a compra") {
                cartRepository.saveCart(action.cart)
                reload()
            }
            is CartsAction.OnCartEdited -> launchSafely("Falha ao editar a compra") {
                cartRepository.updateCart(action.cart)
                reload()
            }

            CartsAction.OnSearchClick -> _state.update { it.copy(isSearchActive = true) }
            is CartsAction.OnQueryChange -> _state.update { it.copy(query = action.value) }
            CartsAction.OnCloseSearch -> _state.update { it.copy(isSearchActive = false, query = "") }
            CartsAction.OnFilterClick -> _state.value.let { sendEvent(CartsEvent.OpenFilter(it.filter, it.carts, it.totalCents)) }
            is CartsAction.OnFilterApplied -> _state.update { it.copy(filter = action.filter) }
            CartsAction.OnClearFilter -> _state.update { it.copy(filter = CartFilter()) }
            CartsAction.OnToggleCompletedSection -> _state.update { it.copy(isCompletedExpanded = !it.isCompletedExpanded) }

            CartsAction.OnSelectAllClick -> _state.update { current ->
                val visibleIds = current.visibleCarts.map { it.firestoreUUID }.toSet()
                // Tudo já selecionado: o mesmo botão limpa a seleção.
                current.copy(selectedIds = if (current.selectedIds.containsAll(visibleIds)) emptySet() else visibleIds)
            }
            CartsAction.OnDeleteSelectedClick -> _state.value.let { current ->
                delete(current.carts.filter { it.firestoreUUID in current.selectedIds })
            }
            CartsAction.OnClearSelection -> _state.update { it.copy(selectedIds = emptySet()) }
        }
    }

    // Voltar fecha a seleção e depois a busca; sem nada aberto, a tela não intercepta o voltar.
    private fun onBack() {
        val current = _state.value
        when {
            current.isSelectionMode -> _state.update { it.copy(selectedIds = emptySet()) }
            current.isSearchActive -> _state.update { it.copy(isSearchActive = false, query = "") }
        }
    }

    private suspend fun reload() {
        val categories = cartRepository.getCategories().associateBy { it.id }
        val carts = cartRepository.getCarts().onEach { it.category = categories[it.categoryId] }
        val progress = cartRepository.getProgress().associateBy { it.cartId }
        val user = userRepository.getUserFromDb()
        val showTotal = configRepository.getIsToShowTotalCart()
        _state.update { current ->
            val ids = carts.map { it.firestoreUUID }.toSet()
            current.copy(
                isLoading = false,
                carts = carts,
                progress = progress,
                user = user,
                showTotal = showTotal,
                // Categoria excluída enquanto a Home estava fora da tela: some do filtro.
                filter = current.filter.copy(categoryIds = current.filter.categoryIds intersect categories.keys),
                selectedIds = current.selectedIds intersect ids,
            )
        }
    }

    private fun toggleSelection(cart: Cart) {
        _state.update {
            val id = cart.firestoreUUID
            it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
        }
    }

    private fun delete(carts: List<Cart>) {
        if (carts.isEmpty()) return
        val ids = carts.map { it.firestoreUUID }.toSet()
        _state.update { current ->
            current.copy(carts = current.carts.filterNot { it.firestoreUUID in ids }, selectedIds = emptySet())
        }
        launchSafely("Falha ao excluir compras") {
            val products = cartRepository.deleteCarts(carts)
            sendEvent(CartsEvent.CartsDeleted(carts, products))
        }
    }

    private fun move(fromKey: Any, toKey: Any) {
        _state.update { current ->
            if (!current.canReorder) return@update current
            val list = current.carts.toMutableList()
            val from = list.indexOfFirst { it.firestoreUUID == fromKey }
            val to = list.indexOfFirst { it.firestoreUUID == toKey }
            if (from < 0 || to < 0) return@update current
            // Arrastar não muda o status da compra: só reordena dentro da mesma seção.
            if (current.isCompleted(list[from]) != current.isCompleted(list[to])) return@update current
            list.add(to, list.removeAt(from))
            current.copy(carts = list)
        }
    }

    private fun saveOrder() {
        val reordered = _state.value.carts.mapIndexed { index, cart -> cart.copyWith(position = index) }
        _state.update { it.copy(carts = reordered) }
        launchSafely("Falha ao salvar a ordem das compras") { cartRepository.updatePosition(reordered) }
    }

    private fun launchSafely(logMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.error(logMessage, e)
                sendEvent(CartsEvent.ShowMessage(UiText.StringResource(R.string.save_change_error)))
                runCatching { reload() }
            }
        }
    }

    private fun sendEvent(event: CartsEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}

/** Cópia que mantém o id local e a categoria, que ficam fora do construtor da entidade. */
private fun Cart.copyWith(position: Int = this.position): Cart =
    copy(position = position).also {
        it.id = id
        it.category = category
    }
