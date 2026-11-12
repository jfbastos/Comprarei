package br.com.zamfir.comprarei.feature.auth.presentation.createaccount

import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTopAppBarDefaults
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiAvatar
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiErrorDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiPasswordRules
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents

@Composable
fun CreateAccountRoot(
    viewModel: CreateAccountViewModel,
    onNavigateBack: () -> Unit,
    onAccountCreated: (photoFailed: Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateAccountEvent.NavigateBack -> onNavigateBack()
            is CreateAccountEvent.AccountCreated -> onAccountCreated(event.photoFailed)
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onAction(CreateAccountAction.OnPicturePicked(it)) }
    }

    CreateAccountScreen(
        state = state,
        onAction = viewModel::onAction,
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    state: CreateAccountState,
    onAction: (CreateAccountAction) -> Unit,
    onPickPhoto: () -> Unit,
) {
    // Enquanto cria a conta, voltar não interrompe o cadastro.
    BackHandler(enabled = state.isCreating) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_account)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(CreateAccountAction.OnBackClick) }, enabled = !state.isCreating) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PictureField(picture = state.picture, enabled = !state.isCreating, onClick = onPickPhoto)

            ComprareiTextField(
                value = state.email,
                onValueChange = { onAction(CreateAccountAction.OnEmailChange(it)) },
                label = stringResource(R.string.user_hint),
                error = state.emailError?.asString(),
                leadingIcon = painterResource(R.drawable.email_black_24dp),
                enabled = !state.isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            ComprareiTextField(
                value = state.name,
                onValueChange = { onAction(CreateAccountAction.OnNameChange(it)) },
                label = stringResource(R.string.name),
                error = state.nameError?.asString(),
                leadingIcon = painterResource(R.drawable.person_black_24dp),
                enabled = !state.isCreating,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            ComprareiTextField(
                value = state.password,
                onValueChange = { onAction(CreateAccountAction.OnPasswordChange(it)) },
                label = stringResource(R.string.password_hint),
                error = state.passwordError?.asString(),
                leadingIcon = painterResource(R.drawable.lock_black_24dp),
                isPassword = true,
                enabled = !state.isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            ComprareiPasswordRules(
                check = state.passwordCheck,
                isEmpty = state.password.isEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            ComprareiTextField(
                value = state.confirmPassword,
                onValueChange = { onAction(CreateAccountAction.OnConfirmPasswordChange(it)) },
                label = stringResource(R.string.reenter_password),
                error = state.confirmPasswordError?.asString(),
                leadingIcon = painterResource(R.drawable.lock_black_24dp),
                isPassword = true,
                enabled = !state.isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(CreateAccountAction.OnCreateClick) }),
                modifier = Modifier.fillMaxWidth(),
            )

            ComprareiButton(
                text = stringResource(R.string.create_account),
                onClick = { onAction(CreateAccountAction.OnCreateClick) },
                isLoading = state.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.already_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onAction(CreateAccountAction.OnBackClick) }, enabled = !state.isCreating) {
                    Text(stringResource(R.string.log_in))
                }
            }
        }
    }

    state.errorDialog?.let { error ->
        ComprareiErrorDialog(
            title = stringResource(R.string.create_account_failed_title),
            message = error.asString(),
            onDismiss = { onAction(CreateAccountAction.OnDismissError) },
        )
    }
}

@Composable
private fun PictureField(picture: Uri?, enabled: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {
        ComprareiAvatar(
            model = picture,
            size = 104.dp,
            contentDescription = stringResource(R.string.profile_change_photo),
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        )
        FilledIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera_24),
                contentDescription = stringResource(R.string.profile_change_photo),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CreateAccountPreview() {
    ComprareiTheme {
        CreateAccountScreen(
            state = CreateAccountState(
                email = "joao@email",
                name = "João",
                password = "abc1",
                emailError = br.com.zamfir.comprarei.core.presentation.UiText.DynamicString("Não é um e-mail válido."),
            ),
            onAction = {},
            onPickPhoto = {},
        )
    }
}
