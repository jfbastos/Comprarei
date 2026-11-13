package br.com.zamfir.comprarei.feature.product.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.CurrencyVisualTransformation
import br.com.zamfir.comprarei.core.util.formatCents
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents

@Composable
fun ProductFormRoot(
    viewModel: ProductFormViewModel,
    onProductCreated: (Product) -> Unit,
    onProductEdited: (Product) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ProductFormEvent.ProductCreated -> onProductCreated(event.product)
            is ProductFormEvent.ProductEdited -> onProductEdited(event.product)
        }
    }

    if (state.isVisible) {
        ProductFormSheet(state = state, onAction = viewModel::onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormSheet(
    state: ProductFormState,
    onAction: (ProductFormAction) -> Unit,
) {
    val hasChanges by rememberUpdatedState(state.hasChanges)
    val currentOnAction by rememberUpdatedState(onAction)

    // Com alterações, arrastar/tocar fora/voltar não fecha: abre a confirmação de descarte.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && hasChanges) {
                currentOnAction(ProductFormAction.OnDismissRequest)
                false
            } else {
                true
            }
        },
    )

    LaunchedEffect(state.isClosing) {
        if (state.isClosing) {
            sheetState.hide()
            onAction(ProductFormAction.OnClosed)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(ProductFormAction.OnClosed) },
        sheetState = sheetState,
    ) {
        ProductFormContent(state = state, onAction = onAction)
    }

    if (state.isDiscardDialogVisible) {
        ComprareiConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            text = stringResource(R.string.discard_changes_message),
            confirmText = stringResource(R.string.discard),
            isDestructive = true,
            onConfirm = { onAction(ProductFormAction.OnConfirmDiscard) },
            onDismiss = { onAction(ProductFormAction.OnCancelDiscard) },
        )
    }
}

@Composable
private fun ProductFormContent(
    state: ProductFormState,
    onAction: (ProductFormAction) -> Unit,
) {
    val currencySymbol = stringResource(R.string.moeda_real)
    val currencyTransformation = remember(currencySymbol) { CurrencyVisualTransformation(currencySymbol) }
    val nameFocusRequester = remember { FocusRequester() }
    val fields = state.fields

    // Foco no nome ao abrir e depois de cada "Salvar e adicionar outro".
    LaunchedEffect(state.lastAddedName) { nameFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (state.isEditing) R.string.edit_product else R.string.new_product),
            style = MaterialTheme.typography.headlineSmall,
        )
        state.lastAddedName?.let {
            Text(
                text = stringResource(R.string.product_added, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ComprareiTextField(
            value = fields.name,
            onValueChange = { onAction(ProductFormAction.OnNameChange(it)) },
            label = stringResource(R.string.name),
            error = state.nameError?.asString(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        ComprareiTextField(
            value = fields.brand,
            onValueChange = { onAction(ProductFormAction.OnBrandChange(it)) },
            label = stringResource(R.string.brand),
            supportingText = stringResource(R.string.optional),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComprareiTextField(
                value = fields.priceCents,
                onValueChange = { onAction(ProductFormAction.OnPriceChange(it)) },
                label = stringResource(R.string.price),
                placeholder = formatCents(0, currencySymbol),
                modifier = Modifier.weight(1f),
                visualTransformation = currencyTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            )
            QuantityStepper(
                quantity = fields.quantity,
                canDecrease = fields.quantityValue > 1,
                onAction = onAction,
            )
        }

        if (fields.priceCents.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.subtotal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCents(fields.subtotalCents, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (!state.isEditing) {
            ComprareiButton(
                text = stringResource(R.string.save_and_add_another),
                onClick = { onAction(ProductFormAction.OnSaveAndAddAnotherClick) },
                modifier = Modifier.fillMaxWidth(),
                style = ComprareiButtonStyle.Secondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ComprareiButton(
                text = stringResource(R.string.cancel),
                onClick = { onAction(ProductFormAction.OnDismissRequest) },
                style = ComprareiButtonStyle.Text,
            )
            ComprareiButton(
                text = stringResource(R.string.save),
                onClick = { onAction(ProductFormAction.OnSaveClick) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: String,
    canDecrease: Boolean,
    onAction: (ProductFormAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilledTonalIconButton(
            onClick = { onAction(ProductFormAction.OnDecreaseQuantity) },
            enabled = canDecrease,
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_remove_24),
                contentDescription = stringResource(R.string.decrease_quantity),
            )
        }
        ComprareiTextField(
            value = quantity,
            onValueChange = { onAction(ProductFormAction.OnQuantityChange(it)) },
            label = stringResource(R.string.quantity_short),
            modifier = Modifier.width(72.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction(ProductFormAction.OnSaveClick) }),
        )
        FilledTonalIconButton(onClick = { onAction(ProductFormAction.OnIncreaseQuantity) }) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.increase_quantity),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductFormNewPreview() {
    ComprareiTheme {
        ProductFormContent(
            state = ProductFormState(
                isVisible = true,
                fields = ProductFormFields(name = "Arroz", brand = "Tio João", priceCents = "2490", quantity = "3"),
                lastAddedName = "Feijão",
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductFormErrorPreview() {
    ComprareiTheme(darkTheme = true) {
        ProductFormContent(
            state = ProductFormState(
                isVisible = true,
                nameError = br.com.zamfir.comprarei.core.presentation.UiText.DynamicString("Não pode ser vazio!"),
            ),
            onAction = {},
        )
    }
}
