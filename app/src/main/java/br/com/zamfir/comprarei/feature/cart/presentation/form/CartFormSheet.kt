package br.com.zamfir.comprarei.feature.cart.presentation.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiColorDot
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CartFormRoot(
    viewModel: CartFormViewModel,
    onCartCreated: (Cart) -> Unit,
    onCartEdited: (Cart) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CartFormEvent.CartCreated -> onCartCreated(event.cart)
            is CartFormEvent.CartEdited -> onCartEdited(event.cart)
        }
    }

    if (state.isVisible) {
        CartFormSheet(state = state, onAction = viewModel::onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartFormSheet(
    state: CartFormState,
    onAction: (CartFormAction) -> Unit,
) {
    val hasChanges by rememberUpdatedState(state.hasChanges)
    val currentOnAction by rememberUpdatedState(onAction)

    // Com alterações, arrastar/tocar fora/voltar não fecha: abre a confirmação de descarte.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && hasChanges) {
                currentOnAction(CartFormAction.OnDismissRequest)
                false
            } else {
                true
            }
        },
    )

    LaunchedEffect(state.isClosing) {
        if (state.isClosing) {
            sheetState.hide()
            onAction(CartFormAction.OnClosed)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(CartFormAction.OnClosed) },
        sheetState = sheetState,
    ) {
        CartFormContent(state = state, onAction = onAction)
    }

    if (state.isDatePickerVisible) {
        CartDatePickerDialog(
            date = state.fields.date,
            onDateSelect = { onAction(CartFormAction.OnDateSelect(it)) },
            onDismiss = { onAction(CartFormAction.OnDatePickerDismiss) },
        )
    }

    if (state.isDiscardDialogVisible) {
        ComprareiConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            text = stringResource(R.string.discard_changes_message),
            confirmText = stringResource(R.string.discard),
            isDestructive = true,
            onConfirm = { onAction(CartFormAction.OnConfirmDiscard) },
            onDismiss = { onAction(CartFormAction.OnCancelDiscard) },
        )
    }
}

@Composable
private fun CartFormContent(
    state: CartFormState,
    onAction: (CartFormAction) -> Unit,
) {
    val fields = state.fields

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (state.isEditing) R.string.edit_cart else R.string.new_cart),
            style = MaterialTheme.typography.headlineSmall,
        )

        ComprareiTextField(
            value = fields.name,
            onValueChange = { onAction(CartFormAction.OnNameChange(it)) },
            label = stringResource(R.string.name),
            error = state.nameError?.asString(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        StoreField(
            store = fields.store,
            suggestions = state.filteredStoreSuggestions,
            onStoreChange = { onAction(CartFormAction.OnStoreChange(it)) },
        )
        DateField(
            date = fields.date,
            onClick = { onAction(CartFormAction.OnDateClick) },
        )
        CategoryField(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = { onAction(CartFormAction.OnCategorySelect(it)) },
        )

        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComprareiButton(
                text = stringResource(R.string.cancel),
                onClick = { onAction(CartFormAction.OnDismissRequest) },
                style = ComprareiButtonStyle.Text,
            )
            ComprareiButton(
                text = stringResource(R.string.save),
                onClick = { onAction(CartFormAction.OnSaveClick) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreField(
    store: String,
    suggestions: List<String>,
    onStoreChange: (String) -> Unit,
) {
    // Estado puramente visual: se o menu de sugestões está aberto.
    var isMenuOpen by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isMenuOpen && suggestions.isNotEmpty(),
        onExpandedChange = { isMenuOpen = it },
    ) {
        ComprareiTextField(
            value = store,
            onValueChange = {
                onStoreChange(it)
                isMenuOpen = true
            },
            label = stringResource(R.string.store_hint),
            supportingText = stringResource(R.string.optional),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )
        ExposedDropdownMenu(
            expanded = isMenuOpen && suggestions.isNotEmpty(),
            onDismissRequest = { isMenuOpen = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onStoreChange(suggestion)
                        isMenuOpen = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun DateField(
    date: LocalDate,
    onClick: () -> Unit,
) {
    // Campo só leitura: o toque abre o calendário.
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) currentOnClick()
        }
    }

    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    ComprareiTextField(
        value = date.format(formatter),
        onValueChange = {},
        label = stringResource(R.string.cart_date),
        supportingText = if (date == LocalDate.now()) stringResource(R.string.today) else null,
        leadingIcon = painterResource(R.drawable.ic_baseline_date_range_24),
        readOnly = true,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    categories: List<Category>,
    selected: Category?,
    onSelect: (categoryId: Int) -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    val hasCategories = categories.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = isMenuOpen && hasCategories,
        onExpandedChange = { if (hasCategories) isMenuOpen = it },
    ) {
        ComprareiTextField(
            value = when {
                !hasCategories -> stringResource(R.string.no_categories)
                selected == null -> stringResource(R.string.none_category)
                else -> selected.description
            },
            onValueChange = {},
            label = stringResource(R.string.category),
            readOnly = true,
            enabled = hasCategories,
            leadingContent = { ComprareiColorDot(color = selected?.color) },
            trailingContent = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuOpen) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = hasCategories),
        )
        ExposedDropdownMenu(
            expanded = isMenuOpen && hasCategories,
            onDismissRequest = { isMenuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.none_category)) },
                leadingIcon = { ComprareiColorDot(color = null) },
                onClick = {
                    onSelect(0)
                    isMenuOpen = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.description) },
                    leadingIcon = { ComprareiColorDot(color = category.color) },
                    onClick = {
                        onSelect(category.id)
                        isMenuOpen = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartDatePickerDialog(
    date: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // O DatePicker trabalha em milissegundos UTC; converter sempre em UTC evita trocar o dia pelo fuso.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Preview(showBackground = true)
@Composable
private fun CartFormNewPreview() {
    ComprareiTheme {
        CartFormContent(
            state = CartFormState(
                isVisible = true,
                fields = CartFormFields(name = "Compra do mês", store = "Atacadão", categoryId = 1),
                categories = listOf(Category("Mercado", 0xFF186F35.toInt(), id = 1)),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CartFormEmptyPreview() {
    ComprareiTheme(darkTheme = true) {
        CartFormContent(
            state = CartFormState(
                isVisible = true,
                editing = Cart(name = "", data = "", total = "", position = 0),
                nameError = br.com.zamfir.comprarei.core.presentation.UiText.DynamicString("Dê um nome ao seu carrinho!"),
            ),
            onAction = {},
        )
    }
}
