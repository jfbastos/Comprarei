package br.com.zamfir.comprarei.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// Verde da marca (#186F35) nas barras do topo e no cabeçalho da Home, nos dois temas:
// no claro é o primary; no escuro o primary vira um verde pastel, e o #186F35 está no primaryContainer.
// Assim os ícones da status bar ficam sempre claros.

val ColorScheme.brandBar: Color
    get() = if (isLight) primary else primaryContainer

// Branco nos dois temas, para a barra ficar idêntica no claro e no escuro.
val ColorScheme.onBrandBar: Color
    get() = if (isLight) onPrimary else Color.White
