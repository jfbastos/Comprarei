package br.com.zamfir.comprarei.feature.cart.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.presentation.UiText
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.util.log.AppLogger
import br.com.zamfir.comprarei.feature.cart.data.CartRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CartFormFields(
    val name: String = "",
    val store: String = "",
    val date: LocalDate = LocalDate.now(),
    // Id local da categoria; 0 = sem categoria (mesma convenção de Cart.categoryId).
    val categoryId: Int = 0,
)

data class CartFormState(
    val isVisible: Boolean = false,
    // Carrinho em edição; nulo no modo "novo".
    val editing: Cart? = null,
    val fields: CartFormFields = CartFormFields(),
    val initialFields: CartFormFields = CartFormFields(),
    val nameError: UiText? = null,
    val categories: List<Category> = emptyList(),
    val storeSuggestions: List<String> = emptyList(),
    val isDatePickerVisible: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
    // Pede ao sheet que anime o fechamento; ao terminar ele envia OnClosed.
    val isClosing: Boolean = false,
) {
    val isEditing: Boolean get() = editing != null
    val hasChanges: Boolean get() = !isClosing && fields != initialFields
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == fields.categoryId }

    /** Lojas já usadas que combinam com o texto digitado (some quando o texto já é uma delas). */
    val filteredStoreSuggestions: List<String>
        get() {
            val typed = fields.store.trim()
            if (storeSuggestions.any { it.equals(typed, ignoreCase = true) }) return emptyList()
            return storeSuggestions.filter { it.contains(typed, ignoreCase = true) }.take(MAX_SUGGESTIONS)
        }
}

sealed interface CartFormAction {
    data class OnOpenNew(val position: Int) : CartFormAction
    data class OnOpenEdit(val cart: Cart) : CartFormAction
    data class OnNameChange(val value: String) : CartFormAction
    data class OnStoreChange(val value: String) : CartFormAction
    data class OnCategorySelect(val categoryId: Int) : CartFormAction
    data object OnDateClick : CartFormAction
    data class OnDateSelect(val date: LocalDate) : CartFormAction
    data object OnDatePickerDismiss : CartFormAction
    data object OnSaveClick : CartFormAction

    /** Arrastar, tocar fora, voltar ou Cancelar. */
    data object OnDismissRequest : CartFormAction
    data object OnConfirmDiscard : CartFormAction
    data object OnCancelDiscard : CartFormAction
    data object OnClosed : CartFormAction
}

sealed interface CartFormEvent {
    data class CartCreated(val cart: Cart) : CartFormEvent
    data class CartEdited(val cart: Cart) : CartFormEvent
}

class CartFormViewModel(private val cartRepository: CartRepository) : ViewModel() {

    private val _state = MutableStateFlow(CartFormState())
    val state: StateFlow<CartFormState> = _state.asStateFlow()

    private val eventChannel = Channel<CartFormEvent>()
    val events = eventChannel.receiveAsFlow()

    private var position = 0

    fun onAction(action: CartFormAction) {
        when (action) {
            is CartFormAction.OnOpenNew -> {
                position = action.position
                _state.value = CartFormState(isVisible = true)
                loadOptions()
            }
            is CartFormAction.OnOpenEdit -> {
                val fields = action.cart.toFields()
                _state.value = CartFormState(
                    isVisible = true,
                    editing = action.cart,
                    fields = fields,
                    initialFields = fields,
                )
                loadOptions()
            }
            is CartFormAction.OnNameChange -> _state.update {
                it.copy(fields = it.fields.copy(name = action.value), nameError = null)
            }
            is CartFormAction.OnStoreChange -> updateFields { copy(store = action.value) }
            is CartFormAction.OnCategorySelect -> updateFields { copy(categoryId = action.categoryId) }
            CartFormAction.OnDateClick -> _state.update { it.copy(isDatePickerVisible = true) }
            is CartFormAction.OnDateSelect -> _state.update {
                it.copy(fields = it.fields.copy(date = action.date), isDatePickerVisible = false)
            }
            CartFormAction.OnDatePickerDismiss -> _state.update { it.copy(isDatePickerVisible = false) }
            CartFormAction.OnSaveClick -> save()
            CartFormAction.OnDismissRequest -> _state.update {
                if (it.hasChanges) it.copy(isDiscardDialogVisible = true) else it.copy(isClosing = true)
            }
            CartFormAction.OnConfirmDiscard -> _state.update { it.copy(isDiscardDialogVisible = false, isClosing = true) }
            CartFormAction.OnCancelDiscard -> _state.update { it.copy(isDiscardDialogVisible = false) }
            CartFormAction.OnClosed -> _state.value = CartFormState()
        }
    }

    private fun updateFields(transform: CartFormFields.() -> CartFormFields) {
        _state.update { it.copy(fields = it.fields.transform()) }
    }

    // Categorias e lojas vêm do banco a cada abertura; o formulário não depende da lista da Home.
    private fun loadOptions() = viewModelScope.launch {
        try {
            val categories = cartRepository.getCategories()
            val stores = cartRepository.getStores()
                .map { it.trim() }
                .distinctBy { it.lowercase() }
            _state.update { it.copy(categories = categories, storeSuggestions = stores) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sem as opções o formulário continua funcionando (categoria vazia, sem sugestões).
            AppLogger.error("Falha ao carregar categorias/lojas do formulário de carrinho", e)
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isClosing) return

        val fields = current.fields
        val name = fields.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(nameError = UiText.StringResource(R.string.name_your_cart)) }
            return
        }

        val category = current.selectedCategory
        val date = fields.date.format(DATE_FORMATTER)
        val editing = current.editing
        val event = if (editing != null) {
            val edited = editing.copy(
                name = name,
                data = date,
                categoryId = category?.id ?: 0,
                store = fields.store.trim(),
            )
            CartFormEvent.CartEdited(edited)
        } else {
            val created = Cart(
                name = name,
                data = date,
                total = Constants.EMPTY_CART_VALUE,
                position = position,
                categoryId = category?.id ?: 0,
                store = fields.store.trim(),
            )
            CartFormEvent.CartCreated(created)
        }
        viewModelScope.launch { eventChannel.send(event) }
        _state.update { it.copy(isClosing = true) }
    }

    private fun Cart.toFields() = CartFormFields(
        name = name,
        store = store,
        date = parseDate(data),
        categoryId = categoryId,
    )

    private fun parseDate(value: String): LocalDate =
        runCatching { LocalDate.parse(value, DATE_FORMATTER) }.getOrDefault(LocalDate.now())
}

// Formato em que Cart.data é gravado no banco e no Firestore; não mudar sem migração.
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(Constants.DATE_PATTERN)
private const val MAX_SUGGESTIONS = 5
