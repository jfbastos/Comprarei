package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/**
 * Fundo com as duas ondas da identidade do app (topo e rodapé), usado no login.
 * Mesmas curvas do antigo `background_signin_screen` (viewport 360x640, esticado na tela),
 * mas nas cores do tema, para funcionar também no escuro.
 */
@Composable
fun ComprareiWaveBackground(modifier: Modifier = Modifier) {
    val waveColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    Canvas(modifier) {
        val sx = size.width / 360f
        val sy = size.height / 640f
        val top = Path().apply {
            moveTo(360f * sx, 0f)
            lineTo(0f, 0f)
            lineTo(0f, 160f * sy)
            cubicTo(0.7f * sx, 157.52f * sy, 23.03f * sx, 78.74f * sy, 117.16f * sx, 79.57f * sy)
            cubicTo(212.79f * sx, 80.43f * sy, 360f * sx, 160f * sy, 360f * sx, 160f * sy)
            close()
        }
        val bottom = Path().apply {
            moveTo(0f, 640f * sy)
            lineTo(360f * sx, 640f * sy)
            lineTo(360f * sx, 494f * sy)
            cubicTo(359.3f * sx, 496.26f * sy, 336.97f * sx, 568.15f * sy, 242.84f * sx, 567.39f * sy)
            cubicTo(147.21f * sx, 566.61f * sy, 0f, 494f * sy, 0f, 494f * sy)
            close()
        }
        drawPath(top, waveColor)
        drawPath(bottom, waveColor)
    }
}

@Preview(widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun ComprareiWaveBackgroundPreview() {
    ComprareiTheme { ComprareiWaveBackground() }
}
