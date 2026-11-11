package br.com.zamfir.comprarei.feature.category.presentation.form

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

data class CategoryFormFields(
    val name: String = "",
    // ARGB, como em Category.color; nulo = nenhuma cor escolhida ainda.
    val color: Int? = null,
)

/** Confirmação de exclusão; [isUsedByCarts] troca o texto para avisar que as compras ficam sem categoria. */
data class CategoryDeleteDialog(val isUsedByCarts: Boolean)

data class CategoryFormState(
    val isVisible: Boolean = false,
    // Categoria em edição; nula no modo "nova".
    val editing: Category? = null,
    val fields: CategoryFormFields = CategoryFormFields(),
    val initialFields: CategoryFormFields = CategoryFormFields(),
    val nameError: UiText? = null,
    val colorError: UiText? = null,
    // Nomes das outras categorias (minúsculos), para validar duplicidade.
    val otherNames: Set<String> = emptySet(),
    val deleteDialog: CategoryDeleteDialog? = null,
    val isDiscardDialogVisible: Boolean = false,
    // Pede ao sheet que anime o fechamento; ao terminar ele envia OnClosed.
    val isClosing: Boolean = false,
) {
    val isEditing: Boolean get() = editing != null
    val hasChanges: Boolean get() = !isClosing && fields != initialFields

    /**
     * Itens da grade de cores, em colunas de [PALETTE_TONES] tons (claro, médio, escuro) do mesmo matiz.
     * Uma cor antiga fora da paleta ocupa a primeira coluna sozinha (os nulos completam a coluna).
     */
    val paletteItems: List<Int?>
        get() {
            val legacy = initialFields.color?.takeIf { it !in CategoryPalette } ?: return CategoryPalette
            return listOf(legacy) + List(PALETTE_TONES - 1) { null } + CategoryPalette
        }
}

sealed interface CategoryFormAction {
    data object OnOpenNew : CategoryFormAction
    data class OnOpenEdit(val category: Category) : CategoryFormAction
    data class OnNameChange(val value: String) : CategoryFormAction
    data class OnColorSelect(val color: Int) : CategoryFormAction
    data object OnSaveClick : CategoryFormAction
    data object OnDeleteClick : CategoryFormAction
    data object OnConfirmDelete : CategoryFormAction
    data object OnCancelDelete : CategoryFormAction

    /** Arrastar, tocar fora, voltar ou Cancelar. */
    data object OnDismissRequest : CategoryFormAction
    data object OnConfirmDiscard : CategoryFormAction
    data object OnCancelDiscard : CategoryFormAction
    data object OnClosed : CategoryFormAction
}

sealed interface CategoryFormEvent {
    data class CategoryCreated(val category: Category) : CategoryFormEvent
    data class CategoryEdited(val category: Category) : CategoryFormEvent

    /** A exclusão já foi confirmada aqui, inclusive o aviso de compras vinculadas. */
    data class CategoryDeleted(val category: Category) : CategoryFormEvent
}

class CategoryFormViewModel(private val categoryRepository: CategoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(CategoryFormState())
    val state: StateFlow<CategoryFormState> = _state.asStateFlow()

    private val eventChannel = Channel<CategoryFormEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: CategoryFormAction) {
        when (action) {
            CategoryFormAction.OnOpenNew -> open(editing = null)
            is CategoryFormAction.OnOpenEdit -> open(editing = action.category)
            is CategoryFormAction.OnNameChange -> _state.update {
                it.copy(fields = it.fields.copy(name = action.value), nameError = null)
            }
            is CategoryFormAction.OnColorSelect -> _state.update {
                it.copy(fields = it.fields.copy(color = action.color), colorError = null)
            }
            CategoryFormAction.OnSaveClick -> save()
            CategoryFormAction.OnDeleteClick -> requestDelete()
            CategoryFormAction.OnConfirmDelete -> {
                val category = _state.value.editing ?: return
                sendEvent(CategoryFormEvent.CategoryDeleted(category))
                _state.update { it.copy(deleteDialog = null, isClosing = true) }
            }
            CategoryFormAction.OnCancelDelete -> _state.update { it.copy(deleteDialog = null) }
            CategoryFormAction.OnDismissRequest -> _state.update {
                if (it.hasChanges) it.copy(isDiscardDialogVisible = true) else it.copy(isClosing = true)
            }
            CategoryFormAction.OnConfirmDiscard -> _state.update { it.copy(isDiscardDialogVisible = false, isClosing = true) }
            CategoryFormAction.OnCancelDiscard -> _state.update { it.copy(isDiscardDialogVisible = false) }
            CategoryFormAction.OnClosed -> _state.value = CategoryFormState()
        }
    }

    private fun open(editing: Category?) {
        val fields = editing?.let { CategoryFormFields(name = it.description, color = it.color) } ?: CategoryFormFields()
        _state.value = CategoryFormState(
            isVisible = true,
            editing = editing,
            fields = fields,
            initialFields = fields,
        )
        viewModelScope.launch {
            try {
                val otherNames = categoryRepository.getCategories()
                    .filter { it.firestoreUUID != editing?.firestoreUUID }
                    .map { it.description.trim().lowercase() }
                    .toSet()
                _state.update { it.copy(otherNames = otherNames) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.error("Falha ao carregar categorias para validar o nome", e)
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isClosing) return

        val name = current.fields.name.trim()
        val color = current.fields.color
        val nameError = when {
            name.isEmpty() -> UiText.StringResource(R.string.blank_name)
            name.lowercase() in current.otherNames -> UiText.StringResource(R.string.category_name_already_exists)
            else -> null
        }
        val colorError = if (color == null) UiText.StringResource(R.string.select_category_color) else null
        if (nameError != null || colorError != null || color == null) {
            _state.update { it.copy(nameError = nameError, colorError = colorError) }
            return
        }

        val editing = current.editing
        val event = if (editing != null) {
            // Cópia em vez de alterar o objeto da lista; id não faz parte do construtor.
            val edited = editing.copy(description = name, color = color).also { it.id = editing.id }
            CategoryFormEvent.CategoryEdited(edited)
        } else {
            CategoryFormEvent.CategoryCreated(Category(description = name, color = color))
        }
        sendEvent(event)
        _state.update { it.copy(isClosing = true) }
    }

    private fun requestDelete() {
        val category = _state.value.editing ?: return
        viewModelScope.launch {
            val isUsed = try {
                categoryRepository.hasCartWithCategorie(category.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Na dúvida, avisa como se houvesse compras vinculadas.
                AppLogger.error("Falha ao verificar compras da categoria", e)
                true
            }
            _state.update { it.copy(deleteDialog = CategoryDeleteDialog(isUsedByCarts = isUsed)) }
        }
    }

    private fun sendEvent(event: CategoryFormEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}

/** Quantidade de tons por matiz na paleta (linhas da grade). */
const val PALETTE_TONES = 3

/**
 * Cores oferecidas para categorias: para cada matiz, os tons 300, 600 e 900 do Material.
 * A ordem é por coluna (os três tons de um matiz em sequência), como a grade horizontal exibe.
 */
val CategoryPalette: List<Int> = listOf(
    0xFFE57373, 0xFFE53935, 0xFFB71C1C, // vermelho
    0xFFF06292, 0xFFD81B60, 0xFF880E4F, // rosa
    0xFFBA68C8, 0xFF8E24AA, 0xFF4A148C, // roxo
    0xFF9575CD, 0xFF5E35B1, 0xFF311B92, // roxo escuro
    0xFF7986CB, 0xFF3949AB, 0xFF1A237E, // índigo
    0xFF64B5F6, 0xFF1E88E5, 0xFF0D47A1, // azul
    0xFF4FC3F7, 0xFF039BE5, 0xFF01579B, // azul claro
    0xFF4DD0E1, 0xFF00ACC1, 0xFF006064, // ciano
    0xFF4DB6AC, 0xFF00897B, 0xFF004D40, // verde-azulado
    0xFF81C784, 0xFF43A047, 0xFF1B5E20, // verde
    0xFFAED581, 0xFF7CB342, 0xFF33691E, // verde claro
    0xFFDCE775, 0xFFC0CA33, 0xFF827717, // lima
    0xFFFFF176, 0xFFFDD835, 0xFFF57F17, // amarelo
    0xFFFFD54F, 0xFFFFB300, 0xFFFF6F00, // âmbar
    0xFFFFB74D, 0xFFFB8C00, 0xFFE65100, // laranja
    0xFFFF8A65, 0xFFF4511E, 0xFFBF360C, // laranja escuro
    0xFFA1887F, 0xFF6D4C41, 0xFF3E2723, // marrom
    0xFF90A4AE, 0xFF546E7A, 0xFF263238, // cinza-azulado
    0xFFBDBDBD, 0xFF757575, 0xFF212121, // cinza
).map { it.toInt() }
