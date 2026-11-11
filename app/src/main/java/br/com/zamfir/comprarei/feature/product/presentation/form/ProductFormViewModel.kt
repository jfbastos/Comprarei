package br.com.zamfir.comprarei.feature.product.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.presentation.CurrencyVisualTransformation
import br.com.zamfir.comprarei.core.presentation.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class ProductFormFields(
    val name: String = "",
    val brand: String = "",
    // Só os dígitos dos centavos ("1250" = 12,50); a máscara é aplicada na exibição.
    val priceCents: String = "",
    val quantity: String = MIN_QUANTITY.toString(),
) {
    val quantityValue: Int get() = quantity.toIntOrNull()?.coerceAtLeast(MIN_QUANTITY) ?: MIN_QUANTITY
    val priceValue: Double get() = (priceCents.toLongOrNull() ?: 0L) / 100.0
    val subtotalCents: Long get() = (priceCents.toLongOrNull() ?: 0L) * quantityValue
}

data class ProductFormState(
    val isVisible: Boolean = false,
    // Produto em edição; nulo no modo "novo".
    val editing: Product? = null,
    val fields: ProductFormFields = ProductFormFields(),
    val initialFields: ProductFormFields = ProductFormFields(),
    val nameError: UiText? = null,
    // Nome do último produto salvo com "Salvar e adicionar outro", exibido como confirmação.
    val lastAddedName: String? = null,
    val isDiscardDialogVisible: Boolean = false,
    // Pede ao sheet que anime o fechamento; ao terminar ele envia OnClosed.
    val isClosing: Boolean = false,
) {
    val isEditing: Boolean get() = editing != null
    val hasChanges: Boolean get() = !isClosing && fields != initialFields
}

sealed interface ProductFormAction {
    data class OnOpenNew(val cartId: Int, val position: Int) : ProductFormAction
    data class OnOpenEdit(val product: Product) : ProductFormAction
    data class OnNameChange(val value: String) : ProductFormAction
    data class OnBrandChange(val value: String) : ProductFormAction
    data class OnPriceChange(val value: String) : ProductFormAction
    data class OnQuantityChange(val value: String) : ProductFormAction
    data object OnIncreaseQuantity : ProductFormAction
    data object OnDecreaseQuantity : ProductFormAction
    data object OnSaveClick : ProductFormAction
    data object OnSaveAndAddAnotherClick : ProductFormAction

    /** Arrastar, tocar fora, voltar ou Cancelar. */
    data object OnDismissRequest : ProductFormAction
    data object OnConfirmDiscard : ProductFormAction
    data object OnCancelDiscard : ProductFormAction
    data object OnClosed : ProductFormAction
}

sealed interface ProductFormEvent {
    data class ProductCreated(val product: Product) : ProductFormEvent
    data class ProductEdited(val product: Product) : ProductFormEvent
}

class ProductFormViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProductFormState())
    val state: StateFlow<ProductFormState> = _state.asStateFlow()

    private val eventChannel = Channel<ProductFormEvent>()
    val events = eventChannel.receiveAsFlow()

    // Destino dos produtos novos; a posição avança a cada "Salvar e adicionar outro".
    private var cartId = -1
    private var nextPosition = 0

    fun onAction(action: ProductFormAction) {
        when (action) {
            is ProductFormAction.OnOpenNew -> {
                cartId = action.cartId
                nextPosition = action.position
                _state.value = ProductFormState(isVisible = true)
            }
            is ProductFormAction.OnOpenEdit -> {
                val fields = action.product.toFields()
                _state.value = ProductFormState(
                    isVisible = true,
                    editing = action.product,
                    fields = fields,
                    initialFields = fields,
                )
            }
            is ProductFormAction.OnNameChange -> _state.update {
                it.copy(fields = it.fields.copy(name = action.value), nameError = null)
            }
            is ProductFormAction.OnBrandChange -> updateFields { copy(brand = action.value) }
            is ProductFormAction.OnPriceChange -> updateFields {
                copy(priceCents = CurrencyVisualTransformation.sanitize(action.value))
            }
            is ProductFormAction.OnQuantityChange -> updateFields {
                copy(quantity = action.value.filter(Char::isDigit).take(MAX_QUANTITY_DIGITS))
            }
            ProductFormAction.OnIncreaseQuantity -> updateFields {
                copy(quantity = (quantityValue + 1).coerceAtMost(MAX_QUANTITY).toString())
            }
            ProductFormAction.OnDecreaseQuantity -> updateFields {
                copy(quantity = (quantityValue - 1).coerceAtLeast(MIN_QUANTITY).toString())
            }
            ProductFormAction.OnSaveClick -> save(addAnother = false)
            ProductFormAction.OnSaveAndAddAnotherClick -> save(addAnother = true)
            ProductFormAction.OnDismissRequest -> _state.update {
                if (it.hasChanges) it.copy(isDiscardDialogVisible = true) else it.copy(isClosing = true)
            }
            ProductFormAction.OnConfirmDiscard -> _state.update { it.copy(isDiscardDialogVisible = false, isClosing = true) }
            ProductFormAction.OnCancelDiscard -> _state.update { it.copy(isDiscardDialogVisible = false) }
            ProductFormAction.OnClosed -> _state.value = ProductFormState()
        }
    }

    private fun updateFields(transform: ProductFormFields.() -> ProductFormFields) {
        _state.update { it.copy(fields = it.fields.transform()) }
    }

    private fun save(addAnother: Boolean) {
        val current = _state.value
        if (current.isClosing) return

        val fields = current.fields
        val name = fields.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(nameError = UiText.StringResource(R.string.can_t_be_blank)) }
            return
        }

        val editing = current.editing
        val event = if (editing != null) {
            // Cópia em vez de alterar o objeto da lista; id não faz parte do construtor.
            val edited = editing.copy(
                name = name,
                brand = fields.brand.trim(),
                price = fields.priceValue,
                quantity = fields.quantityValue,
            ).also { it.id = editing.id }
            ProductFormEvent.ProductEdited(edited)
        } else {
            ProductFormEvent.ProductCreated(
                Product(
                    name = name,
                    brand = fields.brand.trim(),
                    price = fields.priceValue,
                    quantity = fields.quantityValue,
                    cartId = cartId,
                    position = nextPosition++,
                )
            )
        }
        viewModelScope.launch { eventChannel.send(event) }

        _state.update {
            if (addAnother && editing == null) {
                ProductFormState(isVisible = true, lastAddedName = name)
            } else {
                it.copy(isClosing = true)
            }
        }
    }

    private fun Product.toFields() = ProductFormFields(
        name = name,
        brand = brand,
        priceCents = CurrencyVisualTransformation.sanitize((price * 100).roundToLong().toString()),
        quantity = quantity.coerceAtLeast(MIN_QUANTITY).toString(),
    )
}

private const val MIN_QUANTITY = 1
private const val MAX_QUANTITY = 9999
private const val MAX_QUANTITY_DIGITS = 4
