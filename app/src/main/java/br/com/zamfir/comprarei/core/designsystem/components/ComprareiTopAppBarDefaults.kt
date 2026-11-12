package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import br.com.zamfir.comprarei.core.designsystem.theme.brandBar
import br.com.zamfir.comprarei.core.designsystem.theme.onBrandBar

object ComprareiTopAppBarDefaults {

    /** Barra verde da marca, usada no topo de todas as telas. */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun brandColors(): TopAppBarColors {
        val colors = MaterialTheme.colorScheme
        return TopAppBarDefaults.topAppBarColors(
            containerColor = colors.brandBar,
            scrolledContainerColor = colors.brandBar,
            titleContentColor = colors.onBrandBar,
            navigationIconContentColor = colors.onBrandBar,
            actionIconContentColor = colors.onBrandBar,
        )
    }
}
