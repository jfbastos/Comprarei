package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/**
 * Botão principal de criar (nova compra, novo produto, nova categoria), no verde sólido da marca.
 * [expanded] = false recolhe para só o ícone (use ao rolar a lista).
 */
@Composable
fun ComprareiFab(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
        expanded = expanded,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ComprareiFabPreview() {
    ComprareiTheme {
        ComprareiFab(text = "Nova compra", icon = painterResource(R.drawable.ic_add), onClick = {})
    }
}
