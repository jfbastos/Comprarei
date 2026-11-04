package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

enum class ComprareiButtonStyle {
    /** Ação principal da tela (preenchido). */
    Primary,

    /** Ação alternativa (contorno). */
    Secondary,

    /** Ação de baixa ênfase, como "Cancelar". */
    Text,

    /** Ação destrutiva, como excluir (preenchido em vermelho). */
    Destructive,
}

/**
 * Botão padrão do app.
 *
 * @param isLoading troca o texto por um indicador de progresso e ignora cliques, mantendo o tamanho do botão.
 */
@Composable
fun ComprareiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ComprareiButtonStyle = ComprareiButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val handleClick = { if (!isLoading) onClick() }
    val content: @Composable () -> Unit = { ButtonContent(text = text, isLoading = isLoading) }

    when (style) {
        ComprareiButtonStyle.Primary -> Button(
            onClick = handleClick,
            modifier = modifier,
            enabled = enabled,
        ) { content() }

        ComprareiButtonStyle.Secondary -> OutlinedButton(
            onClick = handleClick,
            modifier = modifier,
            enabled = enabled,
        ) { content() }

        ComprareiButtonStyle.Text -> TextButton(
            onClick = handleClick,
            modifier = modifier,
            enabled = enabled,
        ) { content() }

        ComprareiButtonStyle.Destructive -> Button(
            onClick = handleClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { content() }
    }
}

@Composable
private fun ButtonContent(text: String, isLoading: Boolean) {
    // O texto continua no layout (invisível) para o botão não mudar de largura ao carregar.
    Box(contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.alpha(if (isLoading) 0f else 1f))
        if (isLoading) {
            val loadingDescription = stringResource(R.string.cd_loading)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .semantics { contentDescription = loadingDescription },
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComprareiButtonPreview() {
    ComprareiTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComprareiButton(text = "Salvar", onClick = {}, modifier = Modifier.fillMaxWidth())
                ComprareiButton(text = "Salvar", onClick = {}, isLoading = true, modifier = Modifier.fillMaxWidth())
                ComprareiButton(text = "Entrar com Google", onClick = {}, style = ComprareiButtonStyle.Secondary)
                ComprareiButton(text = "Cancelar", onClick = {}, style = ComprareiButtonStyle.Text)
                ComprareiButton(text = "Excluir", onClick = {}, style = ComprareiButtonStyle.Destructive)
                ComprareiButton(text = "Desabilitado", onClick = {}, enabled = false)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComprareiButtonDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComprareiButton(text = "Salvar", onClick = {})
                ComprareiButton(text = "Excluir", onClick = {}, style = ComprareiButtonStyle.Destructive)
            }
        }
    }
}
