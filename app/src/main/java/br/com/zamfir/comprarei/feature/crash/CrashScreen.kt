package br.com.zamfir.comprarei.feature.crash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButton
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiButtonStyle
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

data class CrashState(
    val details: String = "",
    val isDetailsExpanded: Boolean = false,
)

sealed interface CrashAction {
    data object OnRestartClick : CrashAction
    data object OnCopyDetailsClick : CrashAction
    data object OnToggleDetails : CrashAction
}

/**
 * Esta tela não tem ViewModel: só exibe os dados do crash recebidos pelo Intent.
 * O único estado (detalhes abertos/fechados) é visual e fica aqui; as demais ações vão para a Activity.
 */
@Composable
fun CrashRoot(
    details: String,
    onRestart: () -> Unit,
    onCopyDetails: () -> Unit,
) {
    var isDetailsExpanded by rememberSaveable { mutableStateOf(false) }

    CrashScreen(
        state = CrashState(details = details, isDetailsExpanded = isDetailsExpanded),
        onAction = { action ->
            when (action) {
                CrashAction.OnRestartClick -> onRestart()
                CrashAction.OnCopyDetailsClick -> onCopyDetails()
                CrashAction.OnToggleDetails -> isDetailsExpanded = !isDetailsExpanded
            }
        },
    )
}

@Composable
fun CrashScreen(
    state: CrashState,
    onAction: (CrashAction) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ErrorIcon()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.ops_something_went_wrong),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.exception_screen_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                DetailsCard(
                    details = state.details,
                    isExpanded = state.isDetailsExpanded,
                    onToggle = { onAction(CrashAction.OnToggleDetails) },
                    onCopy = { onAction(CrashAction.OnCopyDetailsClick) },
                )
            }
            ComprareiButton(
                text = stringResource(R.string.restart_comprarei),
                onClick = { onAction(CrashAction.OnRestartClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun ErrorIcon() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.round_warning_24_red),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun DetailsCard(
    details: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "chevron")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Stack traces têm linhas longas: rolagem horizontal em vez de quebrar a linha.
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ComprareiButton(
                        text = stringResource(R.string.copy),
                        onClick = onCopy,
                        style = ComprareiButtonStyle.Text,
                    )
                }
            }
        }
    }
}

private val previewDetails = """
    SDK: 34
    Release: 14
    Incremental: U1TDS34.94-12-7-6

    java.lang.IllegalStateException: Exemplo de erro
        at br.com.zamfir.comprarei.feature.cart.presentation.HomeFragment.onViewCreated(HomeFragment.kt:172)
        at androidx.fragment.app.Fragment.performViewCreated(Fragment.java:3128)

    Tue Nov 04 12:00:00 GMT-03:00 2026
""".trimIndent()

@Preview(showBackground = true)
@Composable
private fun CrashScreenPreview() {
    ComprareiTheme {
        CrashScreen(state = CrashState(details = previewDetails), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CrashScreenExpandedDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        CrashScreen(state = CrashState(details = previewDetails, isDetailsExpanded = true), onAction = {})
    }
}
