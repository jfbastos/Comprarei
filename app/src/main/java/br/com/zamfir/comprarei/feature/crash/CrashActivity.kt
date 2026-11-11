package br.com.zamfir.comprarei.feature.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.app.MainActivity
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ícones escuros nas barras do sistema, porque o tema está forçado no claro até a etapa 3.16.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val details = listOf(EXTRA_SOFTWARE, EXTRA_ERROR, EXTRA_DATE)
            .mapNotNull { intent.getStringExtra(it)?.trim()?.takeIf(String::isNotEmpty) }
            .joinToString("\n\n")

        setContent {
            ComprareiTheme {
                CrashRoot(
                    details = details,
                    onRestart = ::restartApp,
                    onCopyDetails = { copyToClipboard(details) },
                )
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun copyToClipboard(details: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.details), details))
        // A partir do Android 13 o próprio sistema mostra a confirmação de cópia.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.details_copied, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_ERROR = "Error"
        const val EXTRA_SOFTWARE = "Software"
        const val EXTRA_DATE = "Date"
    }
}
