package br.com.zamfir.comprarei.feature.cart.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.dao.CartProgress
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.UserInfo
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiAvatar
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiColorDot
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

    // Recarrega ao voltar de Produtos, Categorias ou Configurações (totais, progresso, categorias, "mostrar total").
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(CartsAction.OnResume)
        onPauseOrDispose { }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CartsEvent.OpenCart -> {
                keyboard?.hide()
                onOpenCart(event.cart)
            }
            is CartsEvent.OpenNewCart -> formViewModel.onAction(CartFormAction.OnOpenNew(event.position))
            is CartsEvent.OpenEditCart -> formViewModel.onAction(CartFormAction.OnOpenEdit(event.cart))
            is CartsEvent.OpenFilter -> filterViewModel.onAction(CartFilterAction.OnOpen(event.filter, event.carts))
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
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                SelectionTopBar(count = state.selectedIds.size, onAction = onAction)
            } else {
                CartsTopBar(state = state, onAction = onAction, onMenuClick = onMenuClick)
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode && !state.isLoading) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.new_cart)) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    onClick = { onAction(CartsAction.OnAddClick) },
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
            AnimatedVisibility(visible = state.filter.isActive && !state.isSelectionMode) {
                ActiveFilterChip(onClear = { onAction(CartsAction.OnClearFilter) })
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> ComprareiLoading(modifier = Modifier.align(Alignment.Center))
                    state.carts.isEmpty() -> ComprareiEmptyState(
                        title = stringResource(R.string.empty_list_message),
                        message = stringResource(R.string.carts_empty_message),
                        icon = painterResource(R.drawable.baseline_shopping_cart_24),
                        actionText = stringResource(R.string.new_cart),
                        onAction = { onAction(CartsAction.OnAddClick) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.visibleCarts.isEmpty() -> ComprareiEmptyState(
                        title = stringResource(R.string.carts_search_empty),
                        icon = painterResource(R.drawable.ic_search_24),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> CartList(state = state, listState = listState, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun CartList(
    state: CartsState,
    listState: LazyListState,
    onAction: (CartsAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onAction(CartsAction.OnMove(from.key, to.key))
    }

    LazyColumn(
        state = listState,
        // Espaço no fim para o FAB não cobrir o último item.
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.visibleCarts, key = { it.firestoreUUID }) { cart ->
            ReorderableItem(reorderState, key = cart.firestoreUUID) { isDragging ->
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
    val swipeState = rememberSwipeToDismissBoxState()
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
    progress: CartProgress?,
    showTotal: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    dragHandle: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val category = cart.category
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
                .heightIn(min = 72.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 16.dp, end = if (dragHandle == null) 16.dp else 0.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (category != null) {
                        ComprareiColorDot(color = category.color, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = cart.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                CartProgressRow(progress)
            }
            if (showTotal) {
                Text(
                    text = cart.total,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            dragHandle?.invoke()
        }
    }
}

@Composable
private fun CartProgressRow(progress: CartProgress?) {
    val colors = MaterialTheme.colorScheme
    if (progress == null || progress.total == 0) {
        Text(
            text = stringResource(R.string.carts_no_products),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { progress.done / progress.total.toFloat() },
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .width(72.dp)
                .height(4.dp)
                .clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = pluralStringResource(R.plurals.products_bought_progress, progress.total, progress.done, progress.total),
            style = MaterialTheme.typography.labelMedium,
            color = if (progress.done == progress.total) colors.primary else colors.onSurfaceVariant,
        )
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
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
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
private fun CartsTopBar(state: CartsState, onAction: (CartsAction) -> Unit, onMenuClick: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    // Barra na cor primária para combinar com a status bar verde (até a 3.16).
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
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            } else {
                Text(stringResource(R.string.app_name))
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
        colors = primaryTopBarColors(),
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
        colors = primaryTopBarColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun primaryTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primary,
    titleContentColor = MaterialTheme.colorScheme.onPrimary,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
)

private val previewCategory = Category(description = "Supermercado", color = 0xFF4CAF50.toInt()).apply { id = 1 }

private val previewCarts = listOf(
    Cart(name = "Mercado", data = "12/11/2026", total = "R$ 124,90", position = 0, categoryId = 1, store = "Atacadão")
        .apply { id = 1; category = previewCategory },
    Cart(name = "Obra", data = "10/11/2026", total = "R$ 980,00", position = 1).apply { id = 2 },
    Cart(name = "Farmácia", data = "08/11/2026", total = "R$ 0,00", position = 2).apply { id = 3 },
)

private val previewState = CartsState(
    isLoading = false,
    carts = previewCarts,
    progress = mapOf(1 to CartProgress(1, 7, 3), 2 to CartProgress(2, 4, 4)),
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
