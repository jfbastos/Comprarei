package br.com.zamfir.comprarei.feature.auth.presentation.login

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.luminance
import br.com.zamfir.comprarei.core.presentation.StatusBarIconsEffect
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiWaveBackground
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import br.com.zamfir.comprarei.core.data.firestore.RestoreStep
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiErrorDialog
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiGoogleButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiTextField
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.presentation.ObserveAsEvents
import br.com.zamfir.comprarei.core.data.auth.GoogleAuthClient
import br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword.ForgotPasswordAction
import br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword.ForgotPasswordRoot
import br.com.zamfir.comprarei.feature.auth.presentation.forgotpassword.ForgotPasswordViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SignInRoot(
    viewModel: SignInViewModel,
    forgotPasswordViewModel: ForgotPasswordViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    googleAuthClient: GoogleAuthClient = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            // O Credential Manager mostra a folha de contas sobre a Activity, por isso o pedido sai daqui.
            SignInEvent.RequestGoogleAccount -> scope.launch {
                viewModel.onAction(SignInAction.OnGoogleResult(googleAuthClient.requestIdToken(context)))
            }
            SignInEvent.NavigateToMain -> onNavigateToMain()
            SignInEvent.NavigateToCreateAccount -> onNavigateToCreateAccount()
            SignInEvent.OpenForgotPassword -> forgotPasswordViewModel.onAction(ForgotPasswordAction.OnOpen)
        }
    }

    SignInScreen(state = state, onAction = viewModel::onAction)
    ForgotPasswordRoot(viewModel = forgotPasswordViewModel)
}

@Composable
fun SignInScreen(
    state: SignInState,
    onAction: (SignInAction) -> Unit,
) {
    // Sem barra verde no topo: com fundo claro, os ícones da status bar precisam ser escuros.
    StatusBarIconsEffect(darkIcons = MaterialTheme.colorScheme.surface.luminance() > 0.5f)

    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            // Fundo verde ondulado da identidade do app.
            ComprareiWaveBackground(Modifier.fillMaxSize())
            SignInForm(state = state, onAction = onAction)
        }
    }

    state.restoreStep?.let { step -> RestoreLoading(step = step) }

    state.errorDialog?.let { error ->
        ComprareiErrorDialog(
            title = stringResource(R.string.login_failed_title),
            message = error.asString(),
            onDismiss = { onAction(SignInAction.OnDismissError) },
        )
    }

    if (state.isRestoreFailed) {
        // Sem dispensa por toque fora: a escolha é tentar de novo ou sair.
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.restore_failed_title)) },
            text = { Text(stringResource(R.string.restore_failed_message)) },
            confirmButton = {
                ComprareiButton(
                    text = stringResource(R.string.try_again),
                    onClick = { onAction(SignInAction.OnRetryRestore) },
                )
            },
            dismissButton = {
                ComprareiButton(
                    text = stringResource(R.string.logoff),
                    onClick = { onAction(SignInAction.OnGiveUpRestore) },
                    style = ComprareiButtonStyle.Text,
                )
            },
        )
    }
}

@Composable
private fun SignInForm(
    state: SignInState,
    onAction: (SignInAction) -> Unit,
) {
    val enabled = !state.isBusy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_texto_sem_fundo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .padding(top = 32.dp, bottom = 32.dp)
                .width(260.dp),
        )

        ComprareiTextField(
            value = state.email,
            onValueChange = { onAction(SignInAction.OnEmailChange(it)) },
            label = stringResource(R.string.user_hint),
            error = state.emailError?.asString(),
            leadingIcon = painterResource(R.drawable.email_black_24dp),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        ComprareiTextField(
            value = state.password,
            onValueChange = { onAction(SignInAction.OnPasswordChange(it)) },
            label = stringResource(R.string.password_hint),
            error = state.passwordError?.asString(),
            leadingIcon = painterResource(R.drawable.lock_black_24dp),
            isPassword = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction(SignInAction.OnSignInClick) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { onAction(SignInAction.OnForgotPasswordClick) }, enabled = enabled) {
                Text(stringResource(R.string.forgot_password))
            }
        }

        ComprareiButton(
            text = stringResource(R.string.log_in),
            onClick = { onAction(SignInAction.OnSignInClick) },
            isLoading = state.isSigningIn,
            enabled = enabled || state.isSigningIn,
            modifier = Modifier.fillMaxWidth(),
        )

        OrDivider()

        ComprareiGoogleButton(
            isLoading = state.isGoogleLoading,
            enabled = enabled,
            onClick = { onAction(SignInAction.OnGoogleClick) },
        )

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.dont_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onAction(SignInAction.OnCreateAccountClick) }, enabled = enabled) {
                Text(stringResource(R.string.create_account))
            }
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

private val RestoreIcons = listOf(R.drawable.baseline_shopping_cart_24, R.drawable.products, R.drawable.category_icon_24)

/** Tela cheia enquanto baixa os dados: ícones alternando com fade e a etapa atual. */
@Composable
private fun RestoreLoading(step: RestoreStep) {
    // Voltar não interrompe a restauração.
    BackHandler {}

    var iconIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            iconIndex = (iconIndex + 1) % RestoreIcons.size
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            ComprareiWaveBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Crossfade(targetState = iconIndex, animationSpec = tween(600), label = "restoreIcon") { index ->
                    Icon(
                        painter = painterResource(RestoreIcons[index]),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(96.dp),
                    )
                }
                Text(
                    text = stringResource(step.label()),
                    style = MaterialTheme.typography.titleMedium,
                )
                CircularProgressIndicator()
            }
        }
    }
}

private fun RestoreStep.label(): Int = when (this) {
    RestoreStep.Preparing -> R.string.restore_preparing
    RestoreStep.Carts -> R.string.restore_carts
    RestoreStep.Products -> R.string.restore_products
    RestoreStep.Categories -> R.string.restore_categories
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SignInPreview() {
    ComprareiTheme {
        SignInScreen(state = SignInState(email = "joao@email.com"), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RestoreLoadingPreview() {
    ComprareiTheme {
        RestoreLoading(step = RestoreStep.Products)
    }
}
