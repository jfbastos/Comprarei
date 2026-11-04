package br.com.zamfir.comprarei.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import br.com.zamfir.comprarei.R

// Inter variable font (rsms/inter 4.1, licença OFL): um arquivo com todos os pesos.
@OptIn(ExperimentalTextApi::class)
private fun interFont(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val InterFontFamily = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold),
)

private val Default = Typography()

private fun TextStyle.inter() = copy(fontFamily = InterFontFamily)

// Escala tipográfica padrão do M3, só trocando a fonte. Títulos ganham um peso a mais.
internal val ComprareiTypography = Typography(
    displayLarge = Default.displayLarge.inter(),
    displayMedium = Default.displayMedium.inter(),
    displaySmall = Default.displaySmall.inter(),
    headlineLarge = Default.headlineLarge.inter().copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Default.headlineMedium.inter().copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Default.headlineSmall.inter().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Default.titleLarge.inter().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.inter().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Default.titleSmall.inter(),
    bodyLarge = Default.bodyLarge.inter(),
    bodyMedium = Default.bodyMedium.inter(),
    bodySmall = Default.bodySmall.inter(),
    labelLarge = Default.labelLarge.inter(),
    labelMedium = Default.labelMedium.inter(),
    labelSmall = Default.labelSmall.inter(),
)
