package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/**
 * Diálogo de confirmação com confirmar/cancelar.
 *
 * @param isDestructive pinta o botão de confirmar (e o ícone) com a cor de erro, para exclusões.
 * @param isConfirming mostra carregando no botão de confirmar enquanto a ação roda.
 */
@Composable
fun ComprareiConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.confirm),
    dismissText: String = stringResource(R.string.cancel),
    icon: Painter? = null,
    isDestructive: Boolean = false,
    isConfirming: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isConfirming) onDismiss() },
        modifier = modifier,
        icon = icon?.let {
            {
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
            }
        },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            ComprareiButton(
                text = confirmText,
                onClick = onConfirm,
                style = if (isDestructive) ComprareiButtonStyle.Destructive else ComprareiButtonStyle.Primary,
                isLoading = isConfirming,
            )
        },
        dismissButton = {
            ComprareiButton(
                text = dismissText,
                onClick = onDismiss,
                style = ComprareiButtonStyle.Text,
                enabled = !isConfirming,
            )
        },
    )
}

@Preview
@Composable
private fun ComprareiConfirmDialogPreview() {
    ComprareiTheme {
        ComprareiConfirmDialog(
            title = "Tem certeza?",
            text = "Você realmente deseja excluir 3 itens?",
            confirmText = "Excluir",
            icon = painterResource(R.drawable.ic_delete_24),
            isDestructive = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ComprareiConfirmDialogDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        ComprareiConfirmDialog(
            title = "Fazer backup agora?",
            text = "Seus carrinhos serão enviados para a nuvem.",
            icon = painterResource(R.drawable.baseline_cloud_upload_24),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
