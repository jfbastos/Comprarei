package br.com.zamfir.comprarei.feature.profile.presentation

import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTopAppBarDefaults
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiAvatar
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiErrorDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiPasswordRules
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents

@Composable
fun EditProfileRoot(
    viewModel: EditProfileViewModel,
    initialPicture: Uri?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAction(EditProfileAction.OnLoad(initialPicture)) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EditProfileEvent.NavigateBack -> onNavigateBack()
            EditProfileEvent.Saved -> onSaved()
            EditProfileEvent.NavigateToDeleteAccount -> onNavigateToDeleteAccount()
        }
    }

    // Seletor de fotos do sistema; substitui os listeners estáticos via MainActivity.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onAction(EditProfileAction.OnPicturePicked(it)) }
    }

    EditProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    state: EditProfileState,
    onAction: (EditProfileAction) -> Unit,
    onPickPhoto: () -> Unit,
) {
    // Voltar com alterações pergunta antes de descartar.
    BackHandler(enabled = state.hasChanges && !state.isSaving) { onAction(EditProfileAction.OnBackClick) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editar_perfil)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(EditProfileAction.OnBackClick) }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfilePicture(picture = state.displayedPicture, enabled = !state.isSaving, onClick = onPickPhoto)

            if (state.email.isNotBlank()) {
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ComprareiTextField(
                value = state.name,
                onValueChange = { onAction(EditProfileAction.OnNameChange(it)) },
                label = stringResource(R.string.name),
                error = state.nameError?.asString(),
                enabled = !state.isSaving,
                leadingIcon = painterResource(R.drawable.person_black_24dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.hasPasswordLogin) {
                PasswordSection(state = state, onAction = onAction)
            }

            ComprareiButton(
                text = stringResource(R.string.salvar),
                onClick = { onAction(EditProfileAction.OnSaveClick) },
                enabled = state.canSave,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            TextButton(
                onClick = { onAction(EditProfileAction.OnDeleteAccountClick) },
                enabled = !state.isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(18.dp),
                )
                Text(stringResource(R.string.excluir_conta))
            }
        }
    }

    state.saveError?.let { error ->
        ComprareiErrorDialog(
            title = stringResource(R.string.profile_save_error_title),
            message = error.asString(),
            onDismiss = { onAction(EditProfileAction.OnDismissSaveError) },
        )
    }

    if (state.isDiscardDialogVisible) {
        ComprareiConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            text = stringResource(R.string.discard_changes_message),
            confirmText = stringResource(R.string.discard),
            isDestructive = true,
            onConfirm = { onAction(EditProfileAction.OnConfirmDiscard) },
            onDismiss = { onAction(EditProfileAction.OnCancelDiscard) },
        )
    }
}

@Composable
private fun ProfilePicture(picture: Uri?, enabled: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {
        ComprareiAvatar(
            model = picture,
            size = 120.dp,
            contentDescription = stringResource(R.string.profile_change_photo),
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        )
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera_24),
                contentDescription = stringResource(R.string.profile_change_photo),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PasswordSection(
    state: EditProfileState,
    onAction: (EditProfileAction) -> Unit,
) {
    val arrowRotation by animateFloatAsState(if (state.isPasswordSectionExpanded) 90f else 0f, label = "passwordArrow")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { onAction(EditProfileAction.OnTogglePasswordSection) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.lock_black_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.change_password),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier.rotate(arrowRotation),
            )
        }

        AnimatedVisibility(visible = state.isPasswordSectionExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ComprareiTextField(
                    value = state.currentPassword,
                    onValueChange = { onAction(EditProfileAction.OnCurrentPasswordChange(it)) },
                    label = stringResource(R.string.senha_atual),
                    isPassword = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                ComprareiTextField(
                    value = state.newPassword,
                    onValueChange = { onAction(EditProfileAction.OnNewPasswordChange(it)) },
                    label = stringResource(R.string.nova_senha),
                    isPassword = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                ComprareiPasswordRules(check = state.passwordCheck, isEmpty = state.newPassword.isEmpty())
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun EditProfilePreview() {
    ComprareiTheme {
        EditProfileScreen(
            state = EditProfileState(
                isLoading = false,
                email = "joao@email.com",
                hasPasswordLogin = true,
                name = "João",
                initialName = "João Nascimento",
                isPasswordSectionExpanded = true,
                currentPassword = "segredo",
                newPassword = "abc1",
            ),
            onAction = {},
            onPickPhoto = {},
        )
    }
}
