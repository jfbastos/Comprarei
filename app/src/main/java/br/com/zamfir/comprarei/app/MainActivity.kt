package br.com.zamfir.comprarei.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.app.navigation.AppNavigation
import br.com.zamfir.comprarei.core.data.preferences.ThemeMode
import br.com.zamfir.comprarei.core.designsystem.components.ComprareiConfirmDialog
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.data.workers.BackupWorker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

/** Única Activity do app (fora a CrashActivity): hospeda toda a navegação em Compose. */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.startDestination.value == null }
        // Ícones claros na status bar, sobre as barras verdes (o login escurece os seus enquanto aparece).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Ícones da barra de navegação seguem o tema escolhido no app (que pode diferir do sistema).
            val view = LocalView.current
            LaunchedEffect(darkTheme) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }

            ComprareiTheme(darkTheme = darkTheme) {
                val start = startDestination ?: return@ComprareiTheme

                // Com a restauração pendente, o backup só é agendado quando ela terminar (onLoggedIn).
                LaunchedEffect(Unit) { if (start == StartDestination.Home) startBackupWorker() }
                AppNavigation(startDestination = start, onLoggedIn = ::startBackupWorker)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) NotificationPermission()
            }
        }
    }

    /**
     * Pede a permissão de notificação (avisos de backup) uma única vez. Se o sistema indicar que
     * o usuário já recusou antes, explica o motivo primeiro. Depois disso, só pelas configurações do Android.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    private fun NotificationPermission() {
        var showRationale by rememberSaveable { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

        LaunchedEffect(Unit) {
            if (!viewModel.shouldAskNotificationPermission()) return@LaunchedEffect
            viewModel.onNotificationPermissionAsked()
            val granted = ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            when {
                granted -> Unit
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> showRationale = true
                else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (showRationale) {
            ComprareiConfirmDialog(
                title = stringResource(R.string.permissao_notificacao),
                text = stringResource(R.string.msg_permissao_notificacao),
                icon = painterResource(R.drawable.baseline_cloud_upload_24),
                confirmText = stringResource(R.string.conceder),
                dismissText = stringResource(R.string.nao_conceder),
                onConfirm = {
                    showRationale = false
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onDismiss = { showRationale = false },
            )
        }
    }

    private fun startBackupWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val periodicWorker = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.PERIODIC_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorker,
        )
    }
}
