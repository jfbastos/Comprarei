package br.com.zamfir.comprarei.feature.category.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiColorDot
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiEmptyState
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiLoading
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import br.com.zamfir.comprarei.feature.category.presentation.form.CategoryFormAction
import br.com.zamfir.comprarei.feature.category.presentation.form.CategoryFormRoot
import br.com.zamfir.comprarei.feature.category.presentation.form.CategoryFormViewModel
import kotlinx.coroutines.launch

@Composable
fun CategoriesRoot(
    viewModel: CategoriesViewModel,
    formViewModel: CategoryFormViewModel,
    onNavigateBack: () -> Unit,
    onCategoriesChanged: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onAction(CategoriesAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CategoriesEvent.NavigateBack -> onNavigateBack()
            CategoriesEvent.CategoriesChanged -> onCategoriesChanged()
            is CategoriesEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    CategoriesScreen(
        state = state,
        onAction = viewModel::onAction,
        onNewClick = { formViewModel.onAction(CategoryFormAction.OnOpenNew) },
        onCategoryClick = { formViewModel.onAction(CategoryFormAction.OnOpenEdit(it)) },
        snackbarHostState = snackbarHostState,
    )

    CategoryFormRoot(
        viewModel = formViewModel,
        onCategoryCreated = { viewModel.onAction(CategoriesAction.OnCategoryCreated(it)) },
        onCategoryEdited = { viewModel.onAction(CategoriesAction.OnCategoryEdited(it)) },
        onCategoryDeleted = { viewModel.onAction(CategoriesAction.OnCategoryDeleted(it)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: CategoriesState,
    onAction: (CategoriesAction) -> Unit,
    onNewClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Voltar com a busca aberta fecha só a busca.
    BackHandler(enabled = state.isSearchActive) { onAction(CategoriesAction.OnCloseSearch) }

    val listState = rememberLazyListState()
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        topBar = {
            CategoriesTopBar(
                isSearchActive = state.isSearchActive,
                query = state.query,
                onAction = onAction,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.new_category)) },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                onClick = onNewClick,
                expanded = isFabExpanded,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> ComprareiLoading(modifier = Modifier.align(Alignment.Center))
                state.items.isEmpty() -> ComprareiEmptyState(
                    title = stringResource(R.string.empty_list_message),
                    message = stringResource(R.string.empty_category),
                    icon = painterResource(R.drawable.category_icon_24),
                    actionText = stringResource(R.string.new_category),
                    onAction = onNewClick,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.visibleItems.isEmpty() -> ComprareiEmptyState(
                    title = stringResource(R.string.categories_search_empty),
                    icon = painterResource(R.drawable.ic_search_24),
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    state = listState,
                    // Espaço no fim para o FAB não cobrir o último item.
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.visibleItems, key = { it.category.firestoreUUID }) { item ->
                        CategoryRow(
                            item = item,
                            onClick = { onCategoryClick(item.category) },
                            onDeleteClick = { onAction(CategoriesAction.OnDeleteClick(item)) },
                        )
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { item ->
        val message = stringResource(R.string.delete_category_message, item.category.description) +
            if (item.cartCount > 0) "\n\n" + stringResource(R.string.delete_category_used_warning) else ""
        ComprareiConfirmDialog(
            title = stringResource(R.string.delete_category_title),
            text = message,
            confirmText = stringResource(R.string.delete_action),
            icon = painterResource(R.drawable.ic_delete_24),
            isDestructive = true,
            onConfirm = { onAction(CategoriesAction.OnConfirmDelete) },
            onDismiss = { onAction(CategoriesAction.OnCancelDelete) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesTopBar(
    isSearchActive: Boolean,
    query: String,
    onAction: (CategoriesAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Barra na cor primária para combinar com a status bar verde das telas em XML (até a 3.16).
    TopAppBar(
        title = {
            if (isSearchActive) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TextField(
                    value = query,
                    onValueChange = { onAction(CategoriesAction.OnQueryChange(it)) },
                    placeholder = { Text(stringResource(R.string.search_categories)) },
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
                Text(stringResource(R.string.categories))
            }
        },
        navigationIcon = {
            IconButton(onClick = { onAction(CategoriesAction.OnBackClick) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
        actions = {
            if (isSearchActive) {
                IconButton(onClick = { onAction(CategoriesAction.OnCloseSearch) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_close_24),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            } else {
                IconButton(onClick = { onAction(CategoriesAction.OnSearchClick) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24),
                        contentDescription = stringResource(R.string.search_categories),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun CategoryRow(
    item: CategoryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(item.category.description) },
        supportingContent = {
            Text(
                if (item.cartCount == 0) stringResource(R.string.category_no_carts)
                else pluralStringResource(R.plurals.category_cart_count, item.cartCount, item.cartCount)
            )
        },
        leadingContent = { ComprareiColorDot(color = item.category.color, modifier = Modifier.size(24.dp)) },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24),
                    contentDescription = stringResource(R.string.delete_category_title_short),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenPreview() {
    ComprareiTheme {
        CategoriesScreen(
            state = CategoriesState(
                isLoading = false,
                items = listOf(
                    CategoryItem(Category("Casa", 0xFF1E88E5.toInt()), 1),
                    CategoryItem(Category("Mercado", 0xFF43A047.toInt()), 12),
                    CategoryItem(Category("Obra", 0xFFFB8C00.toInt()), 0),
                ),
            ),
            onAction = {},
            onNewClick = {},
            onCategoryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesEmptyPreview() {
    ComprareiTheme(darkTheme = true) {
        CategoriesScreen(
            state = CategoriesState(isLoading = false),
            onAction = {},
            onNewClick = {},
            onCategoryClick = {},
        )
    }
}
