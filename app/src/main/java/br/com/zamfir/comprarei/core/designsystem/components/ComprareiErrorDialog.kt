package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/**
 * Diálogo para informar um erro ao usuário, com um único botão de OK.
 *
 * Mostra só uma mensagem legível; detalhes técnicos (stack trace) vão para o `AppLogger`,
 * nunca para a tela.
 */
@Composable
fun ComprareiErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.ok),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                painter = painterResource(R.drawable.round_warning_24_red),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            ComprareiButton(
                text = confirmText,
                onClick = onDismiss,
                style = ComprareiButtonStyle.Text,
            )
        },
    )
}

@Preview
@Composable
private fun ComprareiErrorDialogPreview() {
    ComprareiTheme {
        ComprareiErrorDialog(
            title = "Não foi possível salvar o perfil",
            message = "A senha atual está incorreta.",
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ComprareiErrorDialogDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        ComprareiErrorDialog(
            title = "Não foi possível salvar o perfil",
            message = "Sem conexão com a internet.",
            onDismiss = {},
        )
    }
}
