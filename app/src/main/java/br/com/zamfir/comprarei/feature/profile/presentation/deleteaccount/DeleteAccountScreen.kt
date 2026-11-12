package br.com.zamfir.comprarei.feature.profile.presentation.deleteaccount

import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTopAppBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.auth.GoogleAuthClient
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiErrorDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiGoogleButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiLoadingOverlay
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DeleteAccountRoot(
    viewModel: DeleteAccountViewModel,
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    googleAuthClient: GoogleAuthClient = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            DeleteAccountEvent.NavigateBack -> onNavigateBack()
            // O Credential Manager mostra a folha de contas sobre a Activity, por isso o pedido sai daqui.
            DeleteAccountEvent.RequestGoogleAccount -> scope.launch {
                viewModel.onAction(DeleteAccountAction.OnGoogleResult(googleAuthClient.requestIdToken(context)))
            }
            DeleteAccountEvent.AccountDeleted -> onAccountDeleted()
        }
    }

    DeleteAccountScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    state: DeleteAccountState,
    onAction: (DeleteAccountAction) -> Unit,
) {
    // Voltar não interrompe a exclusão em andamento.
    BackHandler(enabled = state.isDeleting) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_deletion)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(DeleteAccountAction.OnBackClick) }, enabled = !state.isDeleting) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = ComprareiTopAppBarDefaults.brandColors(),
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.sorry_that_you_re_leaving),
                    style = MaterialTheme.typography.headlineSmall,
                )

                WhatGetsDeletedCard()

                Spacer(Modifier.height(4.dp))
                IdentitySection(state = state, onAction = onAction)

                Spacer(Modifier.height(8.dp))
                ComprareiButton(
                    text = stringResource(R.string.excluir_conta),
                    onClick = { onAction(DeleteAccountAction.OnDeleteClick) },
                    style = ComprareiButtonStyle.Destructive,
                    enabled = state.canDelete,
                    modifier = Modifier.fillMaxWidth(),
                )
                ComprareiButton(
                    text = stringResource(R.string.cancel),
                    onClick = { onAction(DeleteAccountAction.OnBackClick) },
                    style = ComprareiButtonStyle.Text,
                    enabled = !state.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.isDeleting) {
                ComprareiLoadingOverlay(message = stringResource(R.string.delete_account_deleting))
            }
        }
    }

    if (state.isConfirmDialogVisible) {
        ComprareiConfirmDialog(
            title = stringResource(R.string.delete_account_confirm_title),
            text = stringResource(R.string.delete_account_confirm_message),
            confirmText = stringResource(R.string.delete_account_confirm_button),
            icon = painterResource(R.drawable.round_warning_24_red),
            isDestructive = true,
            onConfirm = { onAction(DeleteAccountAction.OnConfirmDelete) },
            onDismiss = { onAction(DeleteAccountAction.OnCancelDelete) },
        )
    }

    state.errorDialog?.let { error ->
        ComprareiErrorDialog(
            title = stringResource(
                if (error.isDeletionError) R.string.delete_account_error_title else R.string.delete_account_reauth_error_title,
            ),
            message = error.message.asString(),
            onDismiss = { onAction(DeleteAccountAction.OnDismissError) },
        )
    }
}

/** Card de alerta com o que some de vez. */
@Composable
private fun WhatGetsDeletedCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.round_warning_24_red),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.delete_account_warning_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            listOf(
                R.string.delete_account_item_account,
                R.string.delete_account_item_data,
                R.string.delete_account_item_backup,
            ).forEach { item ->
                Text(
                    text = "•  " + stringResource(item),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 36.dp),
                )
            }
            Text(
                text = stringResource(R.string.delete_account_irreversible),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 36.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun IdentitySection(
    state: DeleteAccountState,
    onAction: (DeleteAccountAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.delete_account_identity_title),
            style = MaterialTheme.typography.titleMedium,
        )

        AnimatedContent(targetState = state.isReauthenticated, label = "identity") { isReauthenticated ->
            if (isReauthenticated) {
                IdentityConfirmed()
            } else if (!state.isLoading) {
                IdentityForm(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun IdentityConfirmed() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_done_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.delete_account_identity_confirmed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun IdentityForm(
    state: DeleteAccountState,
    onAction: (DeleteAccountAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(
                if (state.hasPasswordLogin) R.string.delete_account_identity_password else R.string.delete_account_identity_google,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.hasPasswordLogin) {
            if (state.email.isNotBlank()) {
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            ComprareiTextField(
                value = state.password,
                onValueChange = { onAction(DeleteAccountAction.OnPasswordChange(it)) },
                label = stringResource(R.string.password_hint),
                error = state.passwordError?.asString(),
                isPassword = true,
                enabled = !state.isBusy,
                leadingIcon = painterResource(R.drawable.lock_black_24dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(DeleteAccountAction.OnConfirmPasswordClick) }),
                modifier = Modifier.fillMaxWidth(),
            )
            ComprareiButton(
                text = stringResource(R.string.delete_account_confirm_identity),
                onClick = { onAction(DeleteAccountAction.OnConfirmPasswordClick) },
                style = ComprareiButtonStyle.Secondary,
                enabled = state.canCheckPassword || state.isCheckingPassword,
                isLoading = state.isCheckingPassword,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.hasPasswordLogin && state.hasGoogleLogin) {
            OrDivider()
        }

        if (state.hasGoogleLogin) {
            ComprareiGoogleButton(
                text = stringResource(R.string.delete_account_continue_with_google),
                onClick = { onAction(DeleteAccountAction.OnGoogleClick) },
                isLoading = state.isGoogleLoading,
                enabled = !state.isBusy,
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.or),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DeleteAccountPasswordPreview() {
    ComprareiTheme {
        DeleteAccountScreen(
            state = DeleteAccountState(isLoading = false, email = "joao@email.com", hasPasswordLogin = true, password = "segredo"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DeleteAccountConfirmedPreview() {
    ComprareiTheme {
        DeleteAccountScreen(
            state = DeleteAccountState(isLoading = false, hasGoogleLogin = true, isReauthenticated = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DeleteAccountGoogleDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        DeleteAccountScreen(
            state = DeleteAccountState(isLoading = false, hasGoogleLogin = true),
            onAction = {},
        )
    }
}
