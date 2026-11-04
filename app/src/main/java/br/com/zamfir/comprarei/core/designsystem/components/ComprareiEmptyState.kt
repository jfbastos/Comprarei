package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/**
 * Estado vazio de uma lista ou tela: ícone, título, mensagem e uma ação opcional.
 * A ação só aparece quando [actionText] e [onAction] são informados.
 */
@Composable
fun ComprareiEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null && onAction != null) {
            ComprareiButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun ComprareiEmptyStatePreview() {
    ComprareiTheme {
        Surface {
            ComprareiEmptyState(
                title = "Nenhum carrinho ainda",
                message = "Crie um carrinho para começar a montar sua lista de compras.",
                icon = painterResource(R.drawable.baseline_shopping_cart_24),
                actionText = "Novo carrinho",
                onAction = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun ComprareiEmptyStateDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        Surface {
            ComprareiEmptyState(
                title = "Nenhum produto",
                message = "Toque em + para adicionar.",
                icon = painterResource(R.drawable.products),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
