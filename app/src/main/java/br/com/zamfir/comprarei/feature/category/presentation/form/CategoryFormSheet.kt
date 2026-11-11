package br.com.zamfir.comprarei.feature.category.presentation.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiColorDot
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents

@Composable
fun CategoryFormRoot(
    viewModel: CategoryFormViewModel,
    onCategoryCreated: (Category) -> Unit,
    onCategoryEdited: (Category) -> Unit,
    onCategoryDeleted: (Category) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CategoryFormEvent.CategoryCreated -> onCategoryCreated(event.category)
            is CategoryFormEvent.CategoryEdited -> onCategoryEdited(event.category)
            is CategoryFormEvent.CategoryDeleted -> onCategoryDeleted(event.category)
        }
    }

    if (state.isVisible) {
        CategoryFormSheet(state = state, onAction = viewModel::onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    state: CategoryFormState,
    onAction: (CategoryFormAction) -> Unit,
) {
    val hasChanges by rememberUpdatedState(state.hasChanges)
    val currentOnAction by rememberUpdatedState(onAction)

    // Com alterações, arrastar/tocar fora/voltar não fecha: abre a confirmação de descarte.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && hasChanges) {
                currentOnAction(CategoryFormAction.OnDismissRequest)
                false
            } else {
                true
            }
        },
    )

    LaunchedEffect(state.isClosing) {
        if (state.isClosing) {
            sheetState.hide()
            onAction(CategoryFormAction.OnClosed)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(CategoryFormAction.OnClosed) },
        sheetState = sheetState,
    ) {
        CategoryFormContent(state = state, onAction = onAction)
    }

    state.deleteDialog?.let { dialog ->
        val name = state.editing?.description.orEmpty()
        val message = stringResource(R.string.delete_category_message, name) +
            if (dialog.isUsedByCarts) "\n\n" + stringResource(R.string.delete_category_used_warning) else ""
        ComprareiConfirmDialog(
            title = stringResource(R.string.delete_category_title),
            text = message,
            confirmText = stringResource(R.string.delete_action),
            icon = painterResource(R.drawable.ic_delete_24),
            isDestructive = true,
            onConfirm = { onAction(CategoryFormAction.OnConfirmDelete) },
            onDismiss = { onAction(CategoryFormAction.OnCancelDelete) },
        )
    }

    if (state.isDiscardDialogVisible) {
        ComprareiConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            text = stringResource(R.string.discard_changes_message),
            confirmText = stringResource(R.string.discard),
            isDestructive = true,
            onConfirm = { onAction(CategoryFormAction.OnConfirmDiscard) },
            onDismiss = { onAction(CategoryFormAction.OnCancelDiscard) },
        )
    }
}

@Composable
private fun CategoryFormContent(
    state: CategoryFormState,
    onAction: (CategoryFormAction) -> Unit,
) {
    val fields = state.fields

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Padding aplicado por item: a grade de cores rola de borda a borda, mas começa alinhada ao resto.
        val horizontalPadding = Modifier.padding(horizontal = 24.dp)

        Text(
            text = stringResource(if (state.isEditing) R.string.edit_category else R.string.new_category),
            style = MaterialTheme.typography.headlineSmall,
            modifier = horizontalPadding,
        )

        ComprareiTextField(
            value = fields.name,
            onValueChange = { onAction(CategoryFormAction.OnNameChange(it)) },
            label = stringResource(R.string.name),
            error = state.nameError?.asString(),
            modifier = horizontalPadding.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        )

        Text(
            text = stringResource(R.string.category_color),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = horizontalPadding,
        )
        ColorPalette(
            items = state.paletteItems,
            selected = fields.color,
            onSelect = { onAction(CategoryFormAction.OnColorSelect(it)) },
        )
        state.colorError?.let {
            Text(
                text = it.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = horizontalPadding,
            )
        }

        CategoryPreview(name = fields.name.trim(), color = fields.color, modifier = horizontalPadding)

        if (state.isEditing) {
            TextButton(
                onClick = { onAction(CategoryFormAction.OnDeleteClick) },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(18.dp),
                )
                Text(stringResource(R.string.delete_category_title_short))
            }
        }

        Row(modifier = horizontalPadding, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ComprareiButton(
                text = stringResource(R.string.cancel),
                onClick = { onAction(CategoryFormAction.OnDismissRequest) },
                style = ComprareiButtonStyle.Text,
            )
            ComprareiButton(
                text = stringResource(R.string.save),
                onClick = { onAction(CategoryFormAction.OnSaveClick) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Grade horizontal de cores: uma coluna por matiz, com os tons claro, médio e escuro.
 * Rola de borda a borda; o contentPadding alinha a primeira e a última coluna aos 24dp do formulário.
 */
@Composable
private fun ColorPalette(
    items: List<Int?>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    // Abre já mostrando a cor selecionada (coluna dela), útil ao editar.
    val selectedColumn = items.indexOf(selected).takeIf { it >= 0 }?.div(PALETTE_TONES) ?: 0
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (selectedColumn - 2).coerceAtLeast(0) * PALETTE_TONES)
    val swatchSize = 40.dp
    val spacing = 10.dp

    LazyHorizontalGrid(
        rows = GridCells.Fixed(PALETTE_TONES),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier
            .fillMaxWidth()
            .height(swatchSize * PALETTE_TONES + spacing * (PALETTE_TONES - 1)),
    ) {
        itemsIndexed(items) { index, color ->
            if (color == null) {
                Spacer(Modifier.size(swatchSize))
            } else {
                ColorSwatch(
                    color = color,
                    isSelected = color == selected,
                    description = stringResource(R.string.category_color_option, index + 1),
                    onClick = { onSelect(color) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    isSelected: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    val swatchColor = Color(color)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
            )
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_done_24),
                contentDescription = null,
                tint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}

/** Como a categoria aparece nas compras: bolinha + nome. */
@Composable
private fun CategoryPreview(name: String, color: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.category_preview),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComprareiColorDot(color = color)
                Text(
                    text = name.ifEmpty { stringResource(R.string.category_name_placeholder) },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (name.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryFormNewPreview() {
    ComprareiTheme {
        CategoryFormContent(
            state = CategoryFormState(
                isVisible = true,
                fields = CategoryFormFields(name = "Mercado", color = CategoryPalette[28]),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryFormEditLegacyPreview() {
    val legacy = 0xFF00FFCC.toInt()
    ComprareiTheme(darkTheme = true) {
        CategoryFormContent(
            state = CategoryFormState(
                isVisible = true,
                editing = Category("Obra", legacy),
                fields = CategoryFormFields(name = "Obra", color = legacy),
                initialFields = CategoryFormFields(name = "Obra", color = legacy),
            ),
            onAction = {},
        )
    }
}
