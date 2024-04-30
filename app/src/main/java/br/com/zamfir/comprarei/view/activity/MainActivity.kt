package br.com.zamfir.comprarei.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.ActivityMainBinding
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import br.com.zamfir.comprarei.worker.BackupWorker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            loginViewModel.hasLoggedUser()
        }

        loginViewModel.userLoggedState.observe(this) { isUserLogged ->
            if (!isUserLogged) {
                this.startActivity(Intent(this, LoginActivity::class.java))
                this.finish()
            } else {
                _binding = ActivityMainBinding.inflate(layoutInflater)

                setContentView(binding.root)

                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = resources.getColor(R.color.primary_green, null)

                startBackupWorker()
            }
        }
    }

    private fun startBackupWorker() {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val periodicWorker = PeriodicWorkRequestBuilder<BackupWorker>(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS
        )
            .setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Backup_Unique_Job",
            ExistingPeriodicWorkPolicy.KEEP, periodicWorker
        )
    }
}