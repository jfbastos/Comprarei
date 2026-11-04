package br.com.zamfir.comprarei.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// O M3 não tem papel de "aviso". Em vez de um CompositionLocal, derivamos do próprio ColorScheme:
// fundo claro → paleta clara, fundo escuro → paleta escura.
private val ColorScheme.isLight: Boolean get() = background.luminance() > 0.5f

val ColorScheme.warning: Color
    get() = if (isLight) WarningPalette.LightWarning else WarningPalette.DarkWarning

val ColorScheme.onWarning: Color
    get() = if (isLight) WarningPalette.LightOnWarning else WarningPalette.DarkOnWarning

val ColorScheme.warningContainer: Color
    get() = if (isLight) WarningPalette.LightWarningContainer else WarningPalette.DarkWarningContainer

val ColorScheme.onWarningContainer: Color
    get() = if (isLight) WarningPalette.LightOnWarningContainer else WarningPalette.DarkOnWarningContainer
