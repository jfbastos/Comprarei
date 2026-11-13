package br.com.zamfir.comprarei.feature.cart.presentation

import br.com.zamfir.comprarei.core.designsystem.theme.brandBar
import br.com.zamfir.comprarei.core.designsystem.theme.onBrandBar
import br.com.zamfir.comprarei.core.designsystem.components.rememberUnsavedSwipeToDismissBoxState
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTopAppBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiFab
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.dao.CartProgress
import br.com.zamfir.comprarei.core.util.formatCents
import br.com.zamfir.comprarei.core.util.toCents
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.UserInfo
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiAvatar
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiEmptyState
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiLoading
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilter
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterAction
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterRoot
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartFilterViewModel
import br.com.zamfir.comprarei.feature.cart.presentation.filter.CartSort
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormAction
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormRoot
import br.com.zamfir.comprarei.feature.cart.presentation.form.CartFormViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CartsRoot(
    viewModel: CartsViewModel,
    formViewModel: CartFormViewModel,
    filterViewModel: CartFilterViewModel,
    onOpenCart: (Cart) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val keyboard = LocalSoftwareKeyboardController.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CartsEvent.OpenCart -> {
                keyboard?.hide()
                onOpenCart(event.cart)
            }
            is CartsEvent.OpenNewCart -> formViewModel.onAction(CartFormAction.OnOpenNew(event.position))
            is CartsEvent.OpenEditCart -> formViewModel.onAction(CartFormAction.OnOpenEdit(event.cart))
            is CartsEvent.OpenFilter -> filterViewModel.onAction(CartFilterAction.OnOpen(event.filter, event.carts, event.totalCents))
            is CartsEvent.CartsDeleted -> scope.launch {
                // Uma exclusão nova substitui o aviso anterior (que perde o desfazer).
                snackbarHostState.currentSnackbarData?.dismiss()
                val count = event.carts.size
                val result = snackbarHostState.showSnackbar(
                    message = resources.getQuantityString(R.plurals.carts_deleted, count, count),
                    actionLabel = resources.getString(R.string.undo),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onAction(CartsAction.OnUndoDelete(event.carts, event.products))
                }
            }
            is CartsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    CartsScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenCategories = onOpenCategories,
        onOpenSettings = onOpenSettings,
        snackbarHostState = snackbarHostState,
    )

    CartFormRoot(
        viewModel = formViewModel,
        onCartCreated = { viewModel.onAction(CartsAction.OnCartCreated(it)) },
        onCartEdited = { viewModel.onAction(CartsAction.OnCartEdited(it)) },
    )
    CartFilterRoot(
        viewModel = filterViewModel,
        onApply = { viewModel.onAction(CartsAction.OnFilterApplied(it)) },
    )
}

@Composable
fun CartsScreen(
    state: CartsState,
    onAction: (CartsAction) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Voltar fecha o drawer, a seleção ou a busca; sem nada disso, sai do app.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = !drawerState.isOpen && (state.isSelectionMode || state.isSearchActive)) {
        onAction(CartsAction.OnBackClick)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !state.isSelectionMode,
        drawerContent = {
            CartsDrawer(
                user = state.user,
                onItemClick = { open ->
                    scope.launch {
                        drawerState.close()
                        open()
                    }
                },
                onOpenCategories = onOpenCategories,
                onOpenSettings = onOpenSettings,
            )
        },
    ) {
        CartsContent(
            state = state,
            onAction = onAction,
            onMenuClick = { scope.launch { drawerState.open() } },
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun CartsContent(
    state: CartsState,
    onAction: (CartsAction) -> Unit,
    onMenuClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val listState = rememberLazyListState()
    val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    // O cabeçalho verde some na busca e na seleção, que precisam da lista inteira.
    val showHeader = !state.isSearchActive && !state.isSelectionMode

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                SelectionTopBar(count = state.selectedIds.size, onAction = onAction)
            } else {
                CartsTopBar(
                    state = state,
                    // Com o cabeçalho na tela, o nome do app fica de fora para não repetir a saudação.
                    showTitle = !showHeader || !isAtTop,
                    onAction = onAction,
                    onMenuClick = onMenuClick,
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode && !state.isLoading) {
                ComprareiFab(
                    text = stringResource(R.string.new_cart),
                    icon = painterResource(R.drawable.ic_add),
                    onClick = { onAction(CartsAction.OnAddClick) },
                    expanded = isAtTop,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isLoading) {
                ComprareiLoading(modifier = Modifier.align(Alignment.Center))
            } else {
                CartList(state = state, showHeader = showHeader, listState = listState, onAction = onAction)
            }
        }
    }
}

@Composable
private fun CartList(
    state: CartsState,
    showHeader: Boolean,
    listState: LazyListState,
    onAction: (CartsAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onAction(CartsAction.OnMove(from.key, to.key))
    }
    val hasSections = state.completedCarts.isNotEmpty()

    // Um item de compra; usado nas duas seções.
    val cartItem: @Composable LazyItemScope.(Cart) -> Unit = { cart ->
        ReorderableItem(reorderState, key = cart.firestoreUUID, modifier = Modifier.padding(horizontal = 12.dp)) { isDragging ->
            // A alça precisa do escopo do ReorderableItem, por isso é montada aqui e passada como slot.
            val dragHandle: @Composable () -> Unit = {
                IconButton(
                    onClick = {},
                    modifier = Modifier.draggableHandle(
                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragStopped = { onAction(CartsAction.OnMoveFinished) },
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag_indicator_24),
                        contentDescription = stringResource(R.string.products_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
            SwipeableCartRow(
                enabled = !state.isSelectionMode,
                onEdit = { onAction(CartsAction.OnEditClick(cart)) },
                onDelete = { onAction(CartsAction.OnDelete(listOf(cart))) },
            ) {
                CartRow(
                    cart = cart,
                    category = state.categories[cart.categoryId],
                    progress = state.progress[cart.id],
                    showTotal = state.showTotal,
                    isSelected = cart.firestoreUUID in state.selectedIds,
                    isDragging = isDragging,
                    dragHandle = if (state.canReorder) dragHandle else null,
                    onClick = { onAction(CartsAction.OnCartClick(cart)) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAction(CartsAction.OnCartLongClick(cart))
                    },
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        // Espaço no fim para o FAB não cobrir o último item.
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (showHeader) {
            item(key = "header") { CartsHeader(state) }
        }
        if (state.filter.isActive && !state.isSelectionMode) {
            item(key = "filter_chip") {
                ActiveFilterChip(onClear = { onAction(CartsAction.OnClearFilter) })
            }
        }

        when {
            state.carts.isEmpty() -> item(key = "empty") {
                ComprareiEmptyState(
                    title = stringResource(R.string.empty_list_message),
                    message = stringResource(R.string.carts_empty_message),
                    icon = painterResource(R.drawable.baseline_shopping_cart_24),
                    actionText = stringResource(R.string.new_cart),
                    onAction = { onAction(CartsAction.OnAddClick) },
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
            state.visibleCarts.isEmpty() -> item(key = "search_empty") {
                ComprareiEmptyState(
                    title = stringResource(R.string.carts_search_empty),
                    icon = painterResource(R.drawable.ic_search_24),
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
            else -> {
                // O título "Em andamento" só aparece quando há a outra seção, para não poluir a lista.
                if (hasSections && state.ongoingCarts.isNotEmpty()) {
                    item(key = "section_ongoing") {
                        SectionHeader(title = stringResource(R.string.carts_section_ongoing), count = state.ongoingCarts.size)
                    }
                }
                items(state.ongoingCarts, key = { it.firestoreUUID }) { cartItem(it) }
                if (hasSections) {
                    item(key = "section_completed") {
                        SectionHeader(
                            title = stringResource(R.string.carts_section_completed),
                            count = state.completedCarts.size,
                            expanded = state.isCompletedExpanded,
                            onClick = { onAction(CartsAction.OnToggleCompletedSection) },
                        )
                    }
                    if (state.isCompletedExpanded) {
                        items(state.completedCarts, key = { it.firestoreUUID }) { cartItem(it) }
                    }
                }
            }
        }
    }
}

/** Cabeçalho verde com saudação e resumo; termina com a onda da tela de login (assinatura visual do app). */
@Composable
private fun CartsHeader(state: CartsState) {
    val colors = MaterialTheme.colorScheme
    val firstName = state.user?.name?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    val waveDepth = 36.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawHeaderWave(colors.brandBar, waveDepth.toPx()) }
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = waveDepth + 12.dp),
    ) {
        Text(
            text = firstName?.let { stringResource(R.string.carts_greeting, it) } ?: stringResource(R.string.ola),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBrandBar,
        )
        Spacer(Modifier.height(16.dp))
        HeaderStat(
            value = state.openCount.toString(),
            label = stringResource(R.string.carts_open_label),
        )
    }
}

@Composable
private fun HeaderStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBrandBar,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBrandBar.copy(alpha = 0.8f),
        )
    }
}

/**
 * Mesma curva do fundo da tela de login (`background_signin_screen`): funda nas pontas e rasa a 1/3 da largura.
 * Uma segunda onda espelhada e translúcida fica por trás, como as duas camadas do login.
 */
private fun DrawScope.drawHeaderWave(color: Color, depth: Float) {
    val echo = wavePath(size.width, size.height, depth, mirrored = true)
    drawPath(echo, color.copy(alpha = 0.3f))
    val main = wavePath(size.width, size.height - depth * 0.35f, depth * 0.8f, mirrored = false)
    drawPath(main, color)
}

private fun wavePath(width: Float, height: Float, depth: Float, mirrored: Boolean): Path {
    val shallow = height - depth
    fun x(fraction: Float) = if (mirrored) width * (1 - fraction) else width * fraction
    // Borda de baixo percorrida da ponta direita do desenho original até a esquerda; o topo fecha a forma.
    return Path().apply {
        moveTo(x(1f), height)
        cubicTo(x(1f), height, x(0.591f), shallow + depth * 0.005f, x(0.325f), shallow)
        cubicTo(x(0.064f), shallow - depth * 0.016f, x(0.002f), height - depth * 0.031f, x(0f), height)
        lineTo(x(0f), 0f)
        lineTo(x(1f), 0f)
        close()
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val rotation by animateFloatAsState(if (expanded == true) 90f else 0f, label = "chevron")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = "$title · $count",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (expanded != null) {
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

/** Arrastar para a direita edita; para a esquerda exclui (com desfazer no snackbar). */
@Composable
private fun SwipeableCartRow(
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val swipeState = rememberUnsavedSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = swipeState,
        gesturesEnabled = enabled,
        modifier = Modifier.clip(MaterialTheme.shapes.large),
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    // A compra continua na lista: volta para o lugar enquanto o formulário abre.
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
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val contentColor = if (isDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        painter = painterResource(if (isDelete) R.drawable.ic_delete_24 else R.drawable.ic_baseline_edit_24),
                        contentDescription = stringResource(if (isDelete) R.string.delete else R.string.edit),
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
private fun CartRow(
    cart: Cart,
    category: Category?,
    progress: CartProgress?,
    showTotal: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    dragHandle: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val details = listOfNotNull(
        category?.description,
        cart.store.takeIf { it.isNotBlank() },
        cart.data.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

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
                .height(IntrinsicSize.Min)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            // Faixa na cor da categoria; sem categoria, um tom neutro.
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(category?.let { Color(it.color) } ?: colors.outlineVariant),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Text(
                    text = cart.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showTotal) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatCents(progress?.totalValue?.toCents() ?: 0L, stringResource(R.string.moeda_real)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                }
            }
            ProgressRing(progress, Modifier.padding(end = if (dragHandle == null) 16.dp else 0.dp))
            dragHandle?.invoke()
        }
    }
}

/** Anel com "comprados/total"; cheio vira um check. */
@Composable
private fun ProgressRing(progress: CartProgress?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val total = progress?.total ?: 0
    val done = progress?.done ?: 0
    val isComplete = total > 0 && done == total
    val description = if (total == 0) {
        stringResource(R.string.carts_no_products)
    } else {
        pluralStringResource(R.plurals.products_bought_progress, total, done, total)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        CircularProgressIndicator(
            progress = { if (total == 0) 0f else done / total.toFloat() },
            strokeWidth = 4.dp,
            trackColor = colors.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            modifier = Modifier.fillMaxSize(),
        )
        if (isComplete) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_done_24),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = if (total == 0) "0" else "$done/$total",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** Mostra que há filtro ativo; tocar limpa (o ícone de filtro reabre o sheet para ajustar). */
@Composable
private fun ActiveFilterChip(onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(stringResource(R.string.carts_filter_active)) },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_close_24),
                contentDescription = stringResource(R.string.carts_clear_filter),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun CartsDrawer(
    user: UserInfo?,
    onItemClick: (open: () -> Unit) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            ComprareiAvatar(model = user?.profilePicture?.takeIf { it.isNotBlank() }, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = user?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.ola),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user?.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.welcome),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.categories)) },
            icon = { Icon(painterResource(R.drawable.category_icon_24), contentDescription = null) },
            selected = false,
            onClick = { onItemClick(onOpenCategories) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.configuration)) },
            icon = { Icon(painterResource(R.drawable.baseline_settings_24), contentDescription = null) },
            selected = false,
            onClick = { onItemClick(onOpenSettings) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartsTopBar(
    state: CartsState,
    showTitle: Boolean,
    onAction: (CartsAction) -> Unit,
    onMenuClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            if (state.isSearchActive) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TextField(
                    value = state.query,
                    onValueChange = { onAction(CartsAction.OnQueryChange(it)) },
                    placeholder = { Text(stringResource(R.string.search_carts)) },
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
                AnimatedVisibility(visible = showTitle, enter = fadeIn(), exit = fadeOut()) {
                    Text(stringResource(R.string.app_name))
                }
            }
        },
        navigationIcon = {
            if (state.isSearchActive) {
                IconButton(onClick = { onAction(CartsAction.OnCloseSearch) }) {
                    Icon(painterResource(R.drawable.ic_baseline_arrow_back_24), contentDescription = stringResource(R.string.close))
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(painterResource(R.drawable.ic_menu_24), contentDescription = stringResource(R.string.open_menu))
                }
            }
        },
        actions = {
            if (state.isSearchActive) {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onAction(CartsAction.OnQueryChange("")) }) {
                        Icon(painterResource(R.drawable.ic_baseline_close_24), contentDescription = stringResource(R.string.close))
                    }
                }
            } else if (state.carts.isNotEmpty()) {
                IconButton(onClick = { onAction(CartsAction.OnSearchClick) }) {
                    Icon(painterResource(R.drawable.ic_search_24), contentDescription = stringResource(R.string.search_carts))
                }
                IconButton(onClick = { onAction(CartsAction.OnFilterClick) }) {
                    BadgedBox(badge = { if (state.filter.isActive) Badge() }) {
                        Icon(painterResource(R.drawable.ic_filter_list_24), contentDescription = stringResource(R.string.filter))
                    }
                }
            }
        },
        colors = ComprareiTopAppBarDefaults.brandColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onAction: (CartsAction) -> Unit) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = { onAction(CartsAction.OnClearSelection) }) {
                Icon(painterResource(R.drawable.ic_baseline_close_24), contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            IconButton(onClick = { onAction(CartsAction.OnSelectAllClick) }) {
                Icon(painterResource(R.drawable.baseline_check_box_24), contentDescription = stringResource(R.string.select_all))
            }
            IconButton(onClick = { onAction(CartsAction.OnDeleteSelectedClick) }) {
                Icon(painterResource(R.drawable.ic_delete_24), contentDescription = stringResource(R.string.delete_selected))
            }
        },
        colors = ComprareiTopAppBarDefaults.brandColors(),
    )
}


private val previewCategory = Category(description = "Supermercado", color = 0xFF4CAF50.toInt(), id = 1)

private val previewCarts = listOf(
    Cart(name = "Mercado", data = "12/11/2026", total = "R$ 124,90", position = 0, categoryId = 1, store = "Atacadão", id = 1),
    Cart(name = "Obra", data = "10/11/2026", total = "R$ 980,00", position = 1, id = 2),
    Cart(name = "Farmácia", data = "08/11/2026", total = "R$ 0,00", position = 2, id = 3),
)

private val previewState = CartsState(
    isLoading = false,
    carts = previewCarts,
    categories = mapOf(previewCategory.id to previewCategory),
    progress = mapOf(1 to CartProgress(1, 7, 3, 124.9), 2 to CartProgress(2, 4, 4, 980.0)),
)

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun CartsScreenPreview() {
    ComprareiTheme {
        CartsScreen(state = previewState, onAction = {}, onOpenCategories = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun CartsFilteredDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        CartsScreen(
            state = previewState.copy(filter = CartFilter(sort = CartSort.Name), selectedIds = emptySet()),
            onAction = {},
            onOpenCategories = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun CartsEmptyPreview() {
    ComprareiTheme {
        CartsScreen(state = CartsState(isLoading = false), onAction = {}, onOpenCategories = {}, onOpenSettings = {})
    }
}
