package br.com.zamfir.comprarei.feature.product.presentation

import br.com.zamfir.comprarei.core.designsystem.theme.onBrandBar
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTopAppBarDefaults
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiEmptyState
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiFab
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiLoading
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import br.com.zamfir.comprarei.core.presentation.Share
import br.com.zamfir.comprarei.core.util.formatMoney
import br.com.zamfir.comprarei.feature.product.presentation.form.ProductFormAction
import br.com.zamfir.comprarei.feature.product.presentation.form.ProductFormRoot
import br.com.zamfir.comprarei.feature.product.presentation.form.ProductFormViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ProductsRoot(
    viewModel: ProductsViewModel,
    formViewModel: ProductFormViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProductsEvent.NavigateBack -> onNavigateBack()
            is ProductsEvent.OpenNewProduct -> formViewModel.onAction(ProductFormAction.OnOpenNew(event.cartId, event.position))
            is ProductsEvent.OpenEditProduct -> formViewModel.onAction(ProductFormAction.OnOpenEdit(event.product))
            is ProductsEvent.ProductsDeleted -> scope.launch {
                // Uma exclusão nova substitui o aviso anterior (que perde o desfazer).
                snackbarHostState.currentSnackbarData?.dismiss()
                val count = event.products.size
                val result = snackbarHostState.showSnackbar(
                    message = resources.getQuantityString(R.plurals.products_deleted, count, count),
                    actionLabel = resources.getString(R.string.undo),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onAction(ProductsAction.OnUndoDelete(event.products))
            }
            is ProductsEvent.Share -> Share().send(buildShareText(context, event.cartName, event.products), context)
            is ProductsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    ProductsScreen(state = state, onAction = viewModel::onAction, snackbarHostState = snackbarHostState)

    ProductFormRoot(
        viewModel = formViewModel,
        onProductCreated = { viewModel.onAction(ProductsAction.OnProductCreated(it)) },
        onProductEdited = { viewModel.onAction(ProductsAction.OnProductEdited(it)) },
    )
}

@Composable
fun ProductsScreen(
    state: ProductsState,
    onAction: (ProductsAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Voltar fecha a seleção ou a busca antes de sair da tela.
    BackHandler(enabled = state.isSelectionMode || state.isSearchActive) { onAction(ProductsAction.OnBackClick) }

    val listState = rememberLazyListState()
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                SelectionTopBar(count = state.selectedIds.size, onAction = onAction)
            } else {
                ProductsTopBar(state = state, onAction = onAction)
            }
        },
        bottomBar = {
            if (state.products.isNotEmpty()) SummaryBar(state)
        },
        floatingActionButton = {
            if (!state.isSelectionMode) {
                ComprareiFab(
                    text = stringResource(R.string.new_product),
                    icon = painterResource(R.drawable.ic_add),
                    onClick = { onAction(ProductsAction.OnAddClick) },
                    expanded = isFabExpanded,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(visible = state.sort != ProductSort.Manual && !state.isSelectionMode) {
                ActiveSortChip(sort = state.sort, onClear = { onAction(ProductsAction.OnSortChange(ProductSort.Manual)) })
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> ComprareiLoading(modifier = Modifier.align(Alignment.Center))
                    state.products.isEmpty() -> ComprareiEmptyState(
                        title = stringResource(R.string.empty_list_message),
                        message = stringResource(R.string.products_empty_message),
                        icon = painterResource(R.drawable.products),
                        actionText = stringResource(R.string.new_product),
                        onAction = { onAction(ProductsAction.OnAddClick) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.visibleProducts.isEmpty() -> ComprareiEmptyState(
                        title = stringResource(R.string.products_search_empty),
                        icon = painterResource(R.drawable.ic_search_24),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> ProductList(state = state, listState = listState, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun ProductList(
    state: ProductsState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAction: (ProductsAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onAction(ProductsAction.OnMove(from.key, to.key))
    }

    LazyColumn(
        state = listState,
        // Espaço no fim para o FAB não cobrir o último item.
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.visibleProducts, key = { it.firestoreUUID }) { product ->
            ReorderableItem(reorderState, key = product.firestoreUUID) { isDragging ->
                // A alça precisa do escopo do ReorderableItem, por isso é montada aqui e passada como slot.
                val dragHandle: @Composable () -> Unit = {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.draggableHandle(
                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragStopped = { onAction(ProductsAction.OnMoveFinished) },
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_drag_indicator_24),
                            contentDescription = stringResource(R.string.products_drag_handle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
                SwipeableProductRow(
                    product = product,
                    enabled = !state.isSelectionMode,
                    onToggleDone = { onAction(ProductsAction.OnToggleDone(product)) },
                    onDelete = { onAction(ProductsAction.OnDelete(listOf(product))) },
                ) {
                    ProductRow(
                        product = product,
                        isSelected = product.firestoreUUID in state.selectedIds,
                        isSelectionMode = state.isSelectionMode,
                        isDragging = isDragging,
                        dragHandle = if (state.canReorder) dragHandle else null,
                        onClick = { onAction(ProductsAction.OnProductClick(product)) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(ProductsAction.OnProductLongClick(product))
                        },
                        onToggleDone = { onAction(ProductsAction.OnToggleDone(product)) },
                    )
                }
            }
        }
    }
}

/** Arrastar para a direita marca/desmarca; para a esquerda exclui (com desfazer no snackbar). */
@Composable
private fun SwipeableProductRow(
    product: Product,
    enabled: Boolean,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val swipeState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = swipeState,
        gesturesEnabled = enabled,
        modifier = Modifier.clip(MaterialTheme.shapes.large),
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleDone()
                    // O item continua na lista: volta para o lugar.
                    scope.launch { swipeState.reset() }
                }
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = {
            val direction = swipeState.dismissDirection
            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val contentColor = if (isDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        painter = painterResource(
                            when {
                                isDelete -> R.drawable.ic_delete_24
                                product.done -> R.drawable.ic_baseline_close_24
                                else -> R.drawable.ic_baseline_done_24
                            },
                        ),
                        contentDescription = stringResource(
                            when {
                                isDelete -> R.string.products_delete
                                product.done -> R.string.products_mark_pending
                                else -> R.string.products_mark_bought
                            },
                        ),
                        tint = contentColor,
                    )
                }
            }
        },
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductRow(
    product: Product,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isDragging: Boolean,
    dragHandle: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleDone: () -> Unit,
) {
    val currency = stringResource(R.string.moeda_real)
    val colors = MaterialTheme.colorScheme
    // Com quantidade 1 o preço unitário repete o subtotal: só a marca aparece.
    val secondaryText = listOfNotNull(
        product.brand.takeIf { it.isNotBlank() },
        if (product.quantity > 1) "${product.quantity} × ${formatMoney(product.price, currency)}" else null,
    ).joinToString(" · ")
    val contentColor = if (product.done) colors.onSurfaceVariant else colors.onSurface
    val decoration = if (product.done) TextDecoration.LineThrough else null

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) colors.secondaryContainer else colors.surfaceContainerLow,
        border = if (isSelected) BorderStroke(1.dp, colors.primary) else null,
        shadowElevation = if (isDragging) 6.dp else 0.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 4.dp, end = if (dragHandle == null) 16.dp else 0.dp, top = 6.dp, bottom = 6.dp),
        ) {
            DoneToggle(
                done = product.done,
                // Na seleção, o toque seleciona o item em vez de marcar.
                enabled = !isSelectionMode,
                onToggle = onToggleDone,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = decoration,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (secondaryText.isNotEmpty()) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = formatMoney(product.subtotal, currency),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                textDecoration = decoration,
                modifier = Modifier.padding(start = 12.dp),
            )
            dragHandle?.invoke()
        }
    }
}

/** Marcador redondo de "comprado", no estilo das listas de tarefas. */
@Composable
private fun DoneToggle(done: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(if (done) R.string.products_mark_pending else R.string.products_mark_bought)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(value = done, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(colors.primary) else Modifier.border(2.dp, colors.outline, CircleShape),
                ),
        ) {
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_done_24),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Mostra a ordenação ativa (quando não é a manual); tocar volta para a manual. */
@Composable
private fun ActiveSortChip(sort: ProductSort, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(stringResource(R.string.products_sorted_by, stringResource(sort.labelRes))) },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_close_24),
                contentDescription = stringResource(R.string.products_clear_sort),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
}

@Composable
private fun SortMenuButton(sort: ProductSort, onAction: (ProductsAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_sort_24), contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProductSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    trailingIcon = if (option == sort) {
                        { Icon(painterResource(R.drawable.ic_baseline_done_24), contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onAction(ProductsAction.OnSortChange(option))
                    },
                )
            }
        }
    }
}

private val ProductSort.labelRes: Int
    get() = when (this) {
        ProductSort.Manual -> R.string.products_sort_manual
        ProductSort.Name -> R.string.products_sort_name
        ProductSort.HighestTotal -> R.string.products_sort_highest
        ProductSort.LowestTotal -> R.string.products_sort_lowest
        ProductSort.Quantity -> R.string.quantity
        ProductSort.PendingFirst -> R.string.products_sort_pending
    }

/** Rodapé fixo: progresso da compra, quanto falta e o total. */
@Composable
private fun SummaryBar(state: ProductsState) {
    val currency = stringResource(R.string.moeda_real)
    val count = state.products.size

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        // A tela vai até a borda: o rodapé fica acima da barra de navegação do sistema.
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { state.doneCount / count.toFloat() },
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = pluralStringResource(R.plurals.products_bought_progress, count, state.doneCount, count),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = if (state.remaining > 0.0) {
                            stringResource(R.string.products_remaining, formatMoney(state.remaining, currency))
                        } else {
                            stringResource(R.string.products_all_bought)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.products_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMoney(state.total, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductsTopBar(state: ProductsState, onAction: (ProductsAction) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            if (state.isSearchActive) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TextField(
                    value = state.query,
                    onValueChange = { onAction(ProductsAction.OnQueryChange(it)) },
                    placeholder = { Text(stringResource(R.string.search_products)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBrandBar,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBrandBar,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onBrandBar.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBrandBar.copy(alpha = 0.7f),
                        cursorColor = MaterialTheme.colorScheme.onBrandBar,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            } else {
                Text(state.cartName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            IconButton(onClick = { onAction(ProductsAction.OnBackClick) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
        actions = {
            if (state.isSearchActive) {
                IconButton(onClick = { onAction(ProductsAction.OnCloseSearch) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_close_24),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            } else if (state.products.isNotEmpty()) {
                IconButton(onClick = { onAction(ProductsAction.OnSearchClick) }) {
                    Icon(painterResource(R.drawable.ic_search_24), contentDescription = stringResource(R.string.search_products))
                }
                SortMenuButton(sort = state.sort, onAction = onAction)
                IconButton(onClick = { onAction(ProductsAction.OnShareClick) }) {
                    Icon(painterResource(R.drawable.ic_share_24), contentDescription = stringResource(R.string.share))
                }
            }
        },
        colors = ComprareiTopAppBarDefaults.brandColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onAction: (ProductsAction) -> Unit) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = { onAction(ProductsAction.OnClearSelection) }) {
                Icon(painterResource(R.drawable.ic_baseline_close_24), contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            IconButton(onClick = { onAction(ProductsAction.OnSelectAllClick) }) {
                Icon(painterResource(R.drawable.baseline_check_box_24), contentDescription = stringResource(R.string.select_all))
            }
            IconButton(onClick = { onAction(ProductsAction.OnDeleteSelectedClick) }) {
                Icon(painterResource(R.drawable.ic_delete_24), contentDescription = stringResource(R.string.delete_selected))
            }
        },
        colors = ComprareiTopAppBarDefaults.brandColors(),
    )
}


/** Texto compartilhado da compra (mesmo formato de antes da migração). */
private fun buildShareText(context: Context, cartName: String, products: List<Product>): String {
    val currency = context.getString(R.string.moeda_real)
    return buildString {
        append("$cartName \n\n")
        products.forEach { product ->
            append(" * ${product.name}")
            if (product.brand.isNotBlank()) append(" - ${product.brand}")
            if (product.price != 0.0) append(" -> ${formatMoney(product.price, currency)}")
            if (product.quantity != 0) append(" x ${product.quantity.toString().padStart(2, '0')}")
            append("\n")
        }
        append("\nTotal : ${formatMoney(products.sumOf { it.subtotal }, currency)}")
    }
}

private val previewProducts = listOf(
    Product(name = "Arroz", brand = "Tio João", price = 25.9, quantity = 1, cartId = 1, position = 0),
    Product(name = "Feijão", price = 8.5, quantity = 2, cartId = 1, done = true, position = 1),
    Product(name = "Café", brand = "Melitta", price = 18.5, quantity = 1, cartId = 1, position = 2),
)

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun ProductsScreenPreview() {
    ComprareiTheme {
        ProductsScreen(
            state = ProductsState(cartName = "Mercado", isLoading = false, products = previewProducts, sort = ProductSort.Name),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun ProductsSelectionDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        ProductsScreen(
            state = ProductsState(
                cartName = "Mercado",
                isLoading = false,
                products = previewProducts,
                selectedIds = setOf(previewProducts.first().firestoreUUID),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun ProductsEmptyPreview() {
    ComprareiTheme {
        ProductsScreen(state = ProductsState(cartName = "Mercado", isLoading = false), onAction = {})
    }
}
