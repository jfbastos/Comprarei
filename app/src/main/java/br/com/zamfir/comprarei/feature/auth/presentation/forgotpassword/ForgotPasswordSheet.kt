package br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.UiText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordRoot(viewModel: ForgotPasswordViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isVisible) {
        ForgotPasswordSheet(state = state, onAction = viewModel::onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordSheet(
    state: ForgotPasswordState,
    onAction: (ForgotPasswordAction) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onAction(ForgotPasswordAction.OnDismiss) },
        sheetState = sheetState,
    ) {
        AnimatedContent(
            targetState = state.sentTo,
            label = "forgotPasswordContent",
        ) { sentTo ->
            if (sentTo == null) {
                FormContent(state = state, onAction = onAction)
            } else {
                SentContent(
                    email = sentTo,
                    onDone = {
                        // Anima o fechamento antes de limpar o estado.
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onAction(ForgotPasswordAction.OnDismiss)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FormContent(
    state: ForgotPasswordState,
    onAction: (ForgotPasswordAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.forgot_password_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.forgot_password_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ComprareiTextField(
            value = state.email,
            onValueChange = { onAction(ForgotPasswordAction.OnEmailChange(it)) },
            label = stringResource(R.string.user_hint),
            modifier = Modifier.fillMaxWidth(),
            error = state.emailError?.asString(),
            leadingIcon = painterResource(R.drawable.email_black_24dp),
            enabled = !state.isSending,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onAction(ForgotPasswordAction.OnSendClick) }),
        )
        ComprareiButton(
            text = stringResource(R.string.forgot_password_send),
            onClick = { onAction(ForgotPasswordAction.OnSendClick) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isSending,
        )
    }
}

@Composable
private fun SentContent(
    email: String,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.email_black_24dp),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.forgot_password_sent_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.forgot_password_sent_message, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ComprareiButton(
            text = stringResource(R.string.forgot_password_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FormContentPreview() {
    ComprareiTheme {
        FormContent(
            state = ForgotPasswordState(
                isVisible = true,
                email = "joao@email",
                emailError = UiText.StringResource(R.string.invalid_email),
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SentContentPreview() {
    ComprareiTheme {
        SentContent(email = "joao@email.com", onDone = {})
    }
}
