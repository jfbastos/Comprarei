package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bolinha com a cor de uma categoria (ARGB, como em `Category.color`).
 * Sem cor, mostra só o contorno, para "sem categoria".
 */
@Composable
fun ComprareiColorDot(
    color: Int?,
    modifier: Modifier = Modifier,
) {
    val base = modifier.size(16.dp)
    if (color == null) {
        Box(base.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape))
    } else {
        Box(base.background(Color(color), CircleShape))
    }
}
