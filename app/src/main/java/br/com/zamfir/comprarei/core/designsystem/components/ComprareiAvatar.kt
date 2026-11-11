package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import coil3.compose.AsyncImage

/**
 * Foto de perfil redonda. Sem foto (ou enquanto carrega / se falhar), mostra um círculo
 * com o ícone de pessoa nas cores do tema.
 *
 * @param model qualquer modelo aceito pelo Coil (Uri, URL, File); nulo = sem foto.
 */
@Composable
fun ComprareiAvatar(
    model: Any?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var showPlaceholder by remember(model) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Icon(
                painter = painterResource(R.drawable.person_black_24dp),
                contentDescription = if (model == null) contentDescription else null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(size * 0.55f),
            )
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onSuccess = { showPlaceholder = false },
                onError = { showPlaceholder = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
private fun ComprareiAvatarPreview() {
    ComprareiTheme {
        ComprareiAvatar(model = null, size = 120.dp)
    }
}
