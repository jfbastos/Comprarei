package br.com.zamfir.comprarei.feature.cart.presentation.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiColorDot
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.CurrencyVisualTransformation
import br.com.zamfir.comprarei.core.util.formatCents
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CartFilterRoot(
    viewModel: CartFilterViewModel,
    onApply: (CartFilter) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CartFilterEvent.Apply -> onApply(event.filter)
        }
    }

    if (state.isVisible) {
        CartFilterSheet(state = state, onAction = viewModel::onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartFilterSheet(
    state: CartFilterState,
    onAction: (CartFilterAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.isClosing) {
        if (state.isClosing) {
            sheetState.hide()
            onAction(CartFilterAction.OnClosed)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(CartFilterAction.OnClosed) },
        sheetState = sheetState,
    ) {
        CartFilterContent(state = state, onAction = onAction)
    }

    if (state.isDateRangePickerVisible) {
        PeriodPickerDialog(
            initialRange = state.dateRange,
            onSelect = { onAction(CartFilterAction.OnDateRangeSelect(it)) },
            onDismiss = { onAction(CartFilterAction.OnDateRangePickerDismiss) },
        )
    }
}

@Composable
private fun CartFilterContent(
    state: CartFilterState,
    onAction: (CartFilterAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAction(CartFilterAction.OnDismissRequest) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_close_24),
                    contentDescription = stringResource(R.string.close),
                )
            }
        }

        Section(title = stringResource(R.string.sort_by)) {
            ChipRow {
                SortOptions.forEach { (sort, label) ->
                    SelectableChip(
                        label = stringResource(label),
                        selected = state.sort == sort,
                        onClick = { onAction(CartFilterAction.OnSortClick(sort)) },
                    )
                }
            }
        }

        Section(title = stringResource(R.string.filter_period)) {
            ChipRow {
                PeriodOptions.forEach { (preset, label) ->
                    SelectableChip(
                        label = stringResource(label),
                        selected = state.selectedPreset == preset,
                        onClick = { onAction(CartFilterAction.OnPresetClick(preset)) },
                    )
                }
                SelectableChip(
                    label = stringResource(R.string.period_custom),
                    selected = state.isCustomPeriod,
                    onClick = { onAction(CartFilterAction.OnCustomPeriodClick) },
                )
            }
            state.dateRange?.let { range ->
                PeriodChip(range = range, onClear = { onAction(CartFilterAction.OnClearPeriod) })
            }
        }

        Section(title = stringResource(R.string.value)) {
            ChipRow {
                OperatorOptions.forEach { (operator, label) ->
                    SelectableChip(
                        label = stringResource(label),
                        selected = state.valueOperator == operator,
                        onClick = { onAction(CartFilterAction.OnOperatorClick(operator)) },
                    )
                }
            }
            state.valueOperator?.let { operator ->
                ValueFields(
                    operator = operator,
                    minCents = state.minCents,
                    maxCents = state.maxCents,
                    onAction = onAction,
                )
            }
        }

        if (state.categories.isNotEmpty()) {
            Section(title = stringResource(R.string.filter_categories)) {
                CategoryChips(
                    categories = state.categories,
                    selectedIds = state.categoryIds,
                    onClick = { onAction(CartFilterAction.OnCategoryClick(it)) },
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComprareiButton(
                text = stringResource(R.string.clear_all),
                onClick = { onAction(CartFilterAction.OnClearAllClick) },
                style = ComprareiButtonStyle.Text,
                enabled = state.draftFilter.isActive,
            )
            val count = state.resultCount
            ComprareiButton(
                text = if (count == 0) {
                    stringResource(R.string.filter_no_results)
                } else {
                    pluralStringResource(R.plurals.filter_show_results, count, count)
                },
                onClick = { onAction(CartFilterAction.OnApplyClick) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        content()
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leadingIcon ?: if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_done_24),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun PeriodChip(range: DateRange, onClear: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text("${range.start.format(formatter)} – ${range.end.format(formatter)}") },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_date_range_24),
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_close_24),
                contentDescription = stringResource(R.string.clear_period),
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun ValueFields(
    operator: ValueOperator,
    minCents: String,
    maxCents: String,
    onAction: (CartFilterAction) -> Unit,
) {
    val currencySymbol = stringResource(R.string.moeda_real)
    val transformation = remember(currencySymbol) { CurrencyVisualTransformation(currencySymbol) }
    val placeholder = formatCents(0, currencySymbol)
    val keyboard = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ComprareiTextField(
            value = minCents,
            onValueChange = { onAction(CartFilterAction.OnMinChange(it)) },
            label = stringResource(if (operator == ValueOperator.Between) R.string.min else R.string.value),
            placeholder = placeholder,
            visualTransformation = transformation,
            keyboardOptions = keyboard,
            modifier = Modifier.weight(1f),
        )
        if (operator == ValueOperator.Between) {
            ComprareiTextField(
                value = maxCents,
                onValueChange = { onAction(CartFilterAction.OnMaxChange(it)) },
                label = stringResource(R.string.max),
                placeholder = placeholder,
                visualTransformation = transformation,
                keyboardOptions = keyboard,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedIds: Set<Int>,
    onClick: (Int) -> Unit,
) {
    ChipRow {
        categories.forEach { category ->
            SelectableChip(
                label = category.description,
                selected = category.id in selectedIds,
                onClick = { onClick(category.id) },
                leadingIcon = { ComprareiColorDot(color = category.color) },
            )
        }
    }
}

/**
 * Seletor de período em tela cheia, como o M3 recomenda para o DateRangePicker no celular.
 * Dentro do DatePickerDialog ele fica estreito demais: o título com as duas datas quebra letra a letra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPickerDialog(
    initialRange: DateRange?,
    onSelect: (DateRange) -> Unit,
    onDismiss: () -> Unit,
) {
    // O picker trabalha em milissegundos UTC; converter sempre em UTC evita trocar o dia pelo fuso.
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.start?.toUtcMillis(),
        initialSelectedEndDateMillis = initialRange?.end?.toUtcMillis(),
    )
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis
    val headlineFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            if (start != null) onSelect(DateRange(start.toLocalDate(), (end ?: start).toLocalDate()))
                        },
                        enabled = start != null,
                    ) { Text(stringResource(R.string.ok)) }
                }
                DateRangePicker(
                    state = pickerState,
                    title = {
                        Text(
                            text = stringResource(R.string.select_dates),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp),
                        )
                    },
                    // Título próprio em titleLarge: o padrão (headlineLarge) quebra linha com as duas datas.
                    headline = {
                        Text(
                            text = "${start?.toLocalDate()?.format(headlineFormatter) ?: stringResource(R.string.period_start)} – " +
                                (end?.toLocalDate()?.format(headlineFormatter) ?: stringResource(R.string.period_end)),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val SortOptions = listOf(
    CartSort.Name to R.string.sort_name,
    CartSort.Newest to R.string.sort_newest,
    CartSort.Oldest to R.string.sort_oldest,
    CartSort.HighestValue to R.string.sort_highest_value,
    CartSort.LowestValue to R.string.sort_lowest_value,
    CartSort.Category to R.string.category,
)

private val PeriodOptions = listOf(
    PeriodPreset.ThisMonth to R.string.period_this_month,
    PeriodPreset.LastMonth to R.string.period_last_month,
    PeriodPreset.Last30Days to R.string.period_last_30_days,
)

private val OperatorOptions = listOf(
    ValueOperator.Equal to R.string.operator_equal,
    ValueOperator.LessThan to R.string.operator_less_than,
    ValueOperator.GreaterThan to R.string.operator_greater_than,
    ValueOperator.Between to R.string.operator_between,
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CartFilterPreview() {
    ComprareiTheme {
        CartFilterContent(
            state = CartFilterState(
                isVisible = true,
                sort = CartSort.Newest,
                dateRange = PeriodPreset.ThisMonth.range(LocalDate.now()),
                valueOperator = ValueOperator.Between,
                minCents = "5000",
                maxCents = "20000",
                categories = listOf(
                    Category("Mercado", 0xFF186F35.toInt()).apply { id = 1 },
                    Category("Obra", 0xFFB3261E.toInt()).apply { id = 2 },
                ),
                categoryIds = setOf(1),
            ),
            onAction = {},
        )
    }
}
