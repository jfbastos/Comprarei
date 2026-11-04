package br.com.zamfir.comprarei.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta gerada com material-color-utilities (SchemeFidelity) a partir da cor da marca #186F35.
// No tema claro o primary é a própria cor da marca, para manter a identidade das telas XML.

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF186F35),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9AEFA7),
    onPrimaryContainer = Color(0xFF005523),
    inversePrimary = Color(0xFF85D993),
    secondary = Color(0xFF47664A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8ECC9),
    onSecondaryContainer = Color(0xFF4D6C50),
    tertiary = Color(0xFF802B40),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9E4257),
    onTertiaryContainer = Color(0xFFFFD1D7),
    background = Color(0xFFF7FBF2),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF7FBF2),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDBE6D8),
    onSurfaceVariant = Color(0xFF40493F),
    surfaceTint = Color(0xFF186F35),
    inverseSurface = Color(0xFF2D322C),
    inverseOnSurface = Color(0xFFEEF2EA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF707A6E),
    outlineVariant = Color(0xFFBFC9BC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF7FBF2),
    surfaceContainer = Color(0xFFEBEFE7),
    surfaceContainerHigh = Color(0xFFE5E9E1),
    surfaceContainerHighest = Color(0xFFE0E4DC),
    surfaceContainerLow = Color(0xFFF1F5EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD7DBD3),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF85D993),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF186F35),
    onPrimaryContainer = Color(0xFF9AEFA7),
    inversePrimary = Color(0xFF186F35),
    secondary = Color(0xFFADCFAE),
    onSecondary = Color(0xFF19361F),
    secondaryContainer = Color(0xFF325036),
    onSecondaryContainer = Color(0xFF9FC1A0),
    tertiary = Color(0xFFFFB2BE),
    onTertiary = Color(0xFF5F1128),
    tertiaryContainer = Color(0xFF9E4257),
    onTertiaryContainer = Color(0xFFFFD1D7),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DC),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DC),
    surfaceVariant = Color(0xFF40493F),
    onSurfaceVariant = Color(0xFFBFC9BC),
    surfaceTint = Color(0xFF85D993),
    inverseSurface = Color(0xFFE0E4DC),
    inverseOnSurface = Color(0xFF2D322C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF899487),
    outlineVariant = Color(0xFF40493F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF363A35),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313630),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceDim = Color(0xFF101510),
)

// Cores de aviso (amarelo), fora do ColorScheme do M3. Paleta tonal gerada a partir de #FFC107.
internal object WarningPalette {
    val LightWarning = Color(0xFF785900)
    val LightOnWarning = Color(0xFFFFFFFF)
    val LightWarningContainer = Color(0xFFFFDF9E)
    val LightOnWarningContainer = Color(0xFF261A00)

    val DarkWarning = Color(0xFFFABD00)
    val DarkOnWarning = Color(0xFF3F2E00)
    val DarkWarningContainer = Color(0xFF5B4300)
    val DarkOnWarningContainer = Color(0xFFFFDF9E)
}
