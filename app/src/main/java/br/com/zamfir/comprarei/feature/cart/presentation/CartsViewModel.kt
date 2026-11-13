package br.com.zamfir.comprarei.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.user.UserRepository
import br.com.zamfir.comprarei.core.database.dao.CartProgress
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartsState(
    val isLoading: Boolean = true,
    // Na ordem salva (posição).
    val carts: List<Cart> = emptyList(),
    // Por id local da categoria.
    val categories: Map<Int, Category> = emptyMap(),
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

    val visibleCarts: List<Cart> = carts.applyFilter(filter, totalCents, categories).search(query)

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

/** O que a Home lê do banco e das preferências, combinado num valor só. */
private data class HomeData(
    val carts: List<Cart>,
    val categories: Map<Int, Category>,
    val progress: Map<Int, CartProgress>,
    val user: UserInfo?,
    val showTotal: Boolean,
)

sealed interface CartsAction {
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

    // Última lista vinda do banco. Mudanças otimistas (excluir, arrastar) voltam a ela se a gravação falhar.
    private var savedCarts: List<Cart> = emptyList()

    // Durante o arrasto a tela mostra a ordem em andamento; uma emissão do banco no meio desfaria o movimento.
    private var isReordering = false

    init {
        // Recupera a foto de perfil se o arquivo local sumiu; o caminho novo chega pelo Flow do usuário.
        viewModelScope.launch { userRepository.ensureLocalProfilePicture() }

        // Tudo o que a Home mostra vem do banco e das preferências: qualquer mudança feita em
        // Produtos, Categorias ou Configurações chega aqui sozinha, sem recarregar ao voltar.
        viewModelScope.launch {
            combine(
                cartRepository.observeCarts(),
                cartRepository.observeCategories(),
                cartRepository.observeProgress(),
                userRepository.observeUser(),
                configRepository.observeShowTotalCart(),
            ) { carts, categories, progress, user, showTotal ->
                HomeData(carts, categories.associateBy { it.id }, progress.associateBy { it.cartId }, user, showTotal)
            }
                .catch { e ->
                    AppLogger.error("Falha ao carregar as compras", e)
                    _state.update { it.copy(isLoading = false) }
                    sendEvent(CartsEvent.ShowMessage(UiText.StringResource(R.string.carts_load_error)))
                }
                .collect { data ->
                    savedCarts = data.carts
                    show(data)
                }
        }
    }

    fun onAction(action: CartsAction) {
        when (action) {
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
            }
            is CartsAction.OnMove -> move(action.fromKey, action.toKey)
            CartsAction.OnMoveFinished -> saveOrder()
            CartsAction.OnAddClick -> {
                val position = _state.value.carts.maxOfOrNull { it.position }?.plus(1) ?: 0
                sendEvent(CartsEvent.OpenNewCart(position))
            }
            // O formulário só devolve a compra; quem salva é a tela. A lista se atualiza pelo Flow.
            is CartsAction.OnCartCreated -> launchSafely("Falha ao salvar a compra") {
                cartRepository.saveCart(action.cart)
            }
            is CartsAction.OnCartEdited -> launchSafely("Falha ao editar a compra") {
                cartRepository.updateCart(action.cart)
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

    private fun show(data: HomeData) {
        _state.update { current ->
            // Durante o arrasto a lista continua a da tela; o resto (progresso, usuário...) atualiza.
            val carts = if (isReordering) current.carts else data.carts
            val ids = carts.map { it.firestoreUUID }.toSet()
            current.copy(
                isLoading = false,
                carts = carts,
                categories = data.categories,
                progress = data.progress,
                user = data.user,
                showTotal = data.showTotal,
                // Categoria excluída em outra tela: some do filtro.
                filter = current.filter.copy(categoryIds = current.filter.categoryIds intersect data.categories.keys),
                selectedIds = current.selectedIds intersect ids,
            )
        }
    }

    private fun showSavedCarts() {
        _state.update { current ->
            val ids = savedCarts.map { it.firestoreUUID }.toSet()
            current.copy(carts = savedCarts, selectedIds = current.selectedIds intersect ids)
        }
    }

    private fun toggleSelection(cart: Cart) {
        _state.update {
            val id = cart.firestoreUUID
            it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
        }
    }

    // Some da lista na hora, para o gesto de deslizar não voltar o item antes do banco responder.
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
        if (!_state.value.canReorder) return
        isReordering = true
        _state.update { current ->
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
        if (!isReordering) return
        val reordered = _state.value.carts.mapIndexed { index, cart -> cart.copy(position = index) }
        _state.update { it.copy(carts = reordered) }
        launchSafely("Falha ao salvar a ordem das compras") {
            try {
                cartRepository.updatePosition(reordered)
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
                sendEvent(CartsEvent.ShowMessage(UiText.StringResource(R.string.save_change_error)))
                // Desfaz a mudança otimista: volta ao que está no banco.
                showSavedCarts()
            }
        }
    }

    private fun sendEvent(event: CartsEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
