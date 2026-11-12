package br.com.zamfir.comprarei.feature.profile.presentation.deleteaccount

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Só hospeda a tela em Compose até os hosts migrarem para Navigation Compose (3.16). */
class DeleteAccountActivity : ComponentActivity() {

    private val viewModel: DeleteAccountViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ícones claros sobre a barra verde no topo; escuros embaixo, porque o tema está forçado no claro até a 3.16.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            ComprareiTheme {
                DeleteAccountRoot(
                    viewModel = viewModel,
                    onNavigateBack = ::finish,
                    onAccountDeleted = ::restartApp,
                )
            }
        }
    }

    // Reabre o app do zero; sem usuário logado, a MainActivity leva ao login.
    // Usa a Activity de entrada do app para não importar telas de outras features.
    private fun restartApp() {
        Toast.makeText(this, R.string.account_deleted, Toast.LENGTH_LONG).show()
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }
}
