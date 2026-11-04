package br.com.zamfir.comprarei.core.designsystem.theme

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

/**
 * Tema do app.
 *
 * O tema escuro já está pronto, mas o padrão é `false` até todas as telas migrarem para Compose
 * (etapa 3.16). Antes disso, telas Compose escuras ficariam misturadas com telas XML claras.
 * Na 3.16 o padrão passa a ser `isSystemInDarkTheme()`.
 */
@Composable
fun ComprareiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = ComprareiTypography,
        shapes = ComprareiShapes,
        content = content,
    )
}
