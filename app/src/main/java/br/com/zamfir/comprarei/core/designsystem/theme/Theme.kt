package br.com.zamfir.comprarei.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Cantos arredondados de 12dp nos campos, como no estilo MyLoginTextStyle das telas XML.
internal val ComprareiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Tema do app. A MainActivity decide [darkTheme] pela escolha em Configurações (padrão: seguir o sistema). */
@Composable
fun ComprareiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = ComprareiTypography,
        shapes = ComprareiShapes,
        content = content,
    )
}
