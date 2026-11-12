package br.com.zamfir.comprarei.feature.settings.presentation

import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTopAppBarDefaults
import br.com.zamfir.comprarei.core.data.preferences.ThemeMode
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.firestore.BackupStatus
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiAvatar
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun SettingsRoot(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (profilePicture: Uri?) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onAction(SettingsAction.OnScreenShown) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onNavigateBack()
            is SettingsEvent.NavigateToProfile -> onNavigateToProfile(event.profilePicture)
            SettingsEvent.NavigateToLogin -> onNavigateToLogin()
            is SettingsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.configuration)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.OnBackClick) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = ComprareiTopAppBarDefaults.brandColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileCard(
                userName = state.userName,
                profilePicture = state.profilePicture,
                onClick = { onAction(SettingsAction.OnEditProfileClick) },
            )

            SectionHeader(stringResource(R.string.gerais))
            SwitchItem(
                icon = painterResource(R.drawable.ic_vertical_align_bottom_24),
                title = stringResource(R.string.move_to_end_of_list),
                checked = state.moveDoneToBottom,
                onCheckedChange = { onAction(SettingsAction.OnMoveDoneToBottomChange(it)) },
            )
            SwitchItem(
                icon = painterResource(R.drawable.ic_functions_24),
                title = stringResource(R.string.mostrar_total_do_carrinho),
                checked = state.showCartTotal,
                onCheckedChange = { onAction(SettingsAction.OnShowCartTotalChange(it)) },
            )

            SectionHeader(stringResource(R.string.settings_appearance))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(stringResource(state.themeMode.labelRes)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_palette_24), contentDescription = null) },
                modifier = Modifier.clickable { onAction(SettingsAction.OnThemeClick) },
            )

            SectionHeader(stringResource(R.string.backup))
            SwitchItem(
                icon = painterResource(R.drawable.ic_autorenew_24),
                title = stringResource(R.string.settings_auto_backup),
                description = stringResource(R.string.settings_auto_backup_description),
                checked = state.autoBackup,
                onCheckedChange = { onAction(SettingsAction.OnAutoBackupChange(it)) },
            )
            BackupItem(
                lastBackup = state.lastBackup,
                status = state.backupStatus,
                onClick = { onAction(SettingsAction.OnBackupClick) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.logoff), color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_logout_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.clickable { onAction(SettingsAction.OnLogoffClick) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    SettingsDialogs(state = state, onAction = onAction)
}

@Composable
private fun ProfileCard(
    userName: String,
    profilePicture: Uri?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ComprareiAvatar(model = profilePicture, size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                if (userName.isNotBlank()) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.editar_perfil),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchItem(
    icon: Painter,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    // A linha inteira alterna o switch; o Switch em si fica só visual.
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = { Icon(painter = icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun BackupItem(
    lastBackup: String,
    status: BackupStatus,
    onClick: () -> Unit,
) {
    val inProgress = status.isInProgress()
    val supporting = when (status) {
        BackupStatus.Running -> stringResource(R.string.settings_backup_running)
        BackupStatus.WaitingForNetwork -> stringResource(R.string.settings_backup_waiting_wifi)
        else -> if (lastBackup.isBlank()) stringResource(R.string.sem_backup_realizado)
        else stringResource(R.string.settings_last_backup, lastBackup)
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.realizar_backup)) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Icon(painter = painterResource(R.drawable.baseline_cloud_upload_24), contentDescription = null)
        },
        trailingContent = if (inProgress) {
            { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
        } else {
            null
        },
        modifier = Modifier.clickable(enabled = !inProgress, onClick = onClick),
    )
}

@Composable
private fun SettingsDialogs(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    when (state.dialog) {
        SettingsDialog.ConfirmBackup -> ComprareiConfirmDialog(
            title = stringResource(R.string.settings_confirm_backup_title),
            text = stringResource(R.string.settings_confirm_backup_message),
            icon = painterResource(R.drawable.baseline_cloud_upload_24),
            confirmText = stringResource(R.string.realizar_backup),
            onConfirm = { onAction(SettingsAction.OnConfirmBackup) },
            onDismiss = { onAction(SettingsAction.OnDismissDialog) },
        )
        SettingsDialog.ConfirmLogoff -> {
            val lastBackupLine = if (state.lastBackup.isBlank()) {
                stringResource(R.string.settings_confirm_logoff_no_backup)
            } else {
                stringResource(R.string.settings_last_backup, state.lastBackup)
            }
            ComprareiConfirmDialog(
                title = stringResource(R.string.settings_confirm_logoff_title),
                text = stringResource(R.string.settings_confirm_logoff_message) + "\n\n" + lastBackupLine,
                icon = painterResource(R.drawable.ic_logout_24),
                confirmText = stringResource(R.string.logoff),
                isDestructive = true,
                isConfirming = state.isLoggingOff,
                onConfirm = { onAction(SettingsAction.OnConfirmLogoff) },
                onDismiss = { onAction(SettingsAction.OnDismissDialog) },
            )
        }
        SettingsDialog.Theme -> ThemeDialog(
            selected = state.themeMode,
            onSelect = { onAction(SettingsAction.OnThemeSelect(it)) },
            onDismiss = { onAction(SettingsAction.OnDismissDialog) },
        )
        null -> Unit
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

/** Escolha única: tocar numa opção já aplica e fecha. */
@Composable
private fun ThemeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_palette_24), contentDescription = null) },
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { onSelect(mode) }),
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(mode.labelRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ComprareiTheme {
        SettingsScreen(
            state = SettingsState(
                userName = "João Nascimento",
                lastBackup = "11/11/2026 14:32",
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenBackupRunningPreview() {
    ComprareiTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsState(
                userName = "João Nascimento",
                backupStatus = BackupStatus.WaitingForNetwork,
            ),
            onAction = {},
        )
    }
}
