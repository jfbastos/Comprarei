package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/** Indicador de carregamento centralizado na área disponível, com mensagem opcional. */
@Composable
fun ComprareiLoading(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val loadingDescription = stringResource(R.string.cd_loading)
    Column(
        modifier = modifier
            .padding(32.dp)
            .semantics(mergeDescendants = true) { contentDescription = message ?: loadingDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Carregamento por cima do conteúdo, com scrim que bloqueia toques.
 * Use dentro de um `Box` junto com o conteúdo, só enquanto a operação roda.
 */
@Composable
fun ComprareiLoadingOverlay(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            // Consome os toques para o conteúdo de baixo não reagir.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            ComprareiLoading(message = message)
        }
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun ComprareiLoadingPreview() {
    ComprareiTheme {
        Surface {
            ComprareiLoading(message = "Restaurando seus dados...", modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true, heightDp = 320)
@Composable
private fun ComprareiLoadingOverlayDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        Surface {
            Box(Modifier.fillMaxSize()) {
                ComprareiEmptyState(title = "Conteúdo da tela", modifier = Modifier.fillMaxSize())
                ComprareiLoadingOverlay(message = "Entrando...")
            }
        }
    }
}
