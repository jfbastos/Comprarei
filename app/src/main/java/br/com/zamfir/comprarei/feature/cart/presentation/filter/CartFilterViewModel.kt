package br.com.zamfir.comprarei.feature.cart.presentation.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.presentation.CurrencyVisualTransformation
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

enum class PeriodPreset {
    ThisMonth, LastMonth, Last30Days;

    fun range(today: LocalDate): DateRange = when (this) {
        ThisMonth -> DateRange(today.withDayOfMonth(1), today)
        LastMonth -> today.minusMonths(1).let { DateRange(it.withDayOfMonth(1), it.withDayOfMonth(it.lengthOfMonth())) }
        Last30Days -> DateRange(today.minusDays(29), today)
    }
}

data class CartFilterState(
    val isVisible: Boolean = false,
    val sort: CartSort? = null,
    val dateRange: DateRange? = null,
    val valueOperator: ValueOperator? = null,
    // Só dígitos dos centavos, como no formulário de produto.
    val minCents: String = "",
    val maxCents: String = "",
    val categoryIds: Set<Int> = emptySet(),
    val categories: List<Category> = emptyList(),
    // Lista completa da Home, usada só para a contagem ao vivo.
    val carts: List<Cart> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val isDateRangePickerVisible: Boolean = false,
    val isClosing: Boolean = false,
) {
    val selectedPreset: PeriodPreset? get() = PeriodPreset.entries.firstOrNull { dateRange == it.range(today) }
    val isCustomPeriod: Boolean get() = dateRange != null && selectedPreset == null

    val draftFilter: CartFilter
        get() = CartFilter(
            sort = sort,
            dateRange = dateRange,
            value = valueFilter(),
            categoryIds = categoryIds,
        )

    val resultCount: Int get() = carts.applyFilter(draftFilter).size

    private fun valueFilter(): ValueFilter? {
        val operator = valueOperator ?: return null
        val min = minCents.toLongOrNull()
        val max = maxCents.toLongOrNull()
        return when {
            operator == ValueOperator.Between && (min != null || max != null) -> ValueFilter(operator, min ?: 0L, max)
            min != null -> ValueFilter(operator, min)
            else -> null
        }
    }
}

sealed interface CartFilterAction {
    data class OnOpen(val filter: CartFilter, val carts: List<Cart>) : CartFilterAction
    data class OnSortClick(val sort: CartSort) : CartFilterAction
    data class OnPresetClick(val preset: PeriodPreset) : CartFilterAction
    data object OnCustomPeriodClick : CartFilterAction
    data class OnDateRangeSelect(val range: DateRange) : CartFilterAction
    data object OnDateRangePickerDismiss : CartFilterAction
    data object OnClearPeriod : CartFilterAction
    data class OnOperatorClick(val operator: ValueOperator) : CartFilterAction
    data class OnMinChange(val value: String) : CartFilterAction
    data class OnMaxChange(val value: String) : CartFilterAction
    data class OnCategoryClick(val categoryId: Int) : CartFilterAction
    data object OnClearAllClick : CartFilterAction
    data object OnApplyClick : CartFilterAction

    /** Arrastar, tocar fora, voltar ou fechar: descarta o rascunho. */
    data object OnDismissRequest : CartFilterAction
    data object OnClosed : CartFilterAction
}

sealed interface CartFilterEvent {
    data class Apply(val filter: CartFilter) : CartFilterEvent
}

class CartFilterViewModel(private val cartRepository: CartRepository) : ViewModel() {

    private val _state = MutableStateFlow(CartFilterState())
    val state: StateFlow<CartFilterState> = _state.asStateFlow()

    private val eventChannel = Channel<CartFilterEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: CartFilterAction) {
        when (action) {
            is CartFilterAction.OnOpen -> open(action.filter, action.carts)
            // Tocar no chip já selecionado desmarca.
            is CartFilterAction.OnSortClick -> _state.update { it.copy(sort = action.sort.takeIf { sort -> sort != it.sort }) }
            is CartFilterAction.OnPresetClick -> _state.update {
                val range = action.preset.range(it.today)
                it.copy(dateRange = range.takeIf { r -> r != it.dateRange })
            }
            CartFilterAction.OnCustomPeriodClick -> _state.update { it.copy(isDateRangePickerVisible = true) }
            is CartFilterAction.OnDateRangeSelect -> _state.update {
                it.copy(dateRange = action.range, isDateRangePickerVisible = false)
            }
            CartFilterAction.OnDateRangePickerDismiss -> _state.update { it.copy(isDateRangePickerVisible = false) }
            CartFilterAction.OnClearPeriod -> _state.update { it.copy(dateRange = null) }
            is CartFilterAction.OnOperatorClick -> _state.update {
                it.copy(valueOperator = action.operator.takeIf { op -> op != it.valueOperator })
            }
            is CartFilterAction.OnMinChange -> _state.update { it.copy(minCents = CurrencyVisualTransformation.sanitize(action.value)) }
            is CartFilterAction.OnMaxChange -> _state.update { it.copy(maxCents = CurrencyVisualTransformation.sanitize(action.value)) }
            is CartFilterAction.OnCategoryClick -> _state.update {
                val ids = if (action.categoryId in it.categoryIds) it.categoryIds - action.categoryId else it.categoryIds + action.categoryId
                it.copy(categoryIds = ids)
            }
            CartFilterAction.OnClearAllClick -> _state.update {
                it.copy(sort = null, dateRange = null, valueOperator = null, minCents = "", maxCents = "", categoryIds = emptySet())
            }
            CartFilterAction.OnApplyClick -> {
                val current = _state.value
                if (current.isClosing) return
                viewModelScope.launch { eventChannel.send(CartFilterEvent.Apply(current.draftFilter)) }
                _state.update { it.copy(isClosing = true) }
            }
            CartFilterAction.OnDismissRequest -> _state.update { it.copy(isClosing = true) }
            CartFilterAction.OnClosed -> _state.value = CartFilterState()
        }
    }

    private fun open(filter: CartFilter, carts: List<Cart>) {
        val value = filter.value
        _state.value = CartFilterState(
            isVisible = true,
            sort = filter.sort,
            dateRange = filter.dateRange,
            valueOperator = value?.operator,
            minCents = value?.minCents?.takeIf { it > 0 || value.operator != ValueOperator.Between }?.toString().orEmpty(),
            maxCents = value?.maxCents?.toString().orEmpty(),
            categoryIds = filter.categoryIds,
            carts = carts,
        )
        viewModelScope.launch {
            try {
                val categories = cartRepository.getCategories()
                _state.update { state ->
                    // Descarta categorias que não existem mais.
                    state.copy(categories = categories, categoryIds = state.categoryIds.filterTo(mutableSetOf()) { id -> categories.any { it.id == id } })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.error("Falha ao carregar categorias do filtro", e)
            }
        }
    }
}
