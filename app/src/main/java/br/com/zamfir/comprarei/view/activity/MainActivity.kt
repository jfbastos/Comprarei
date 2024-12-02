package br.com.zamfir.comprarei.view.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.data.workers.BackupWorker
import br.com.zamfir.comprarei.databinding.ActivityMainBinding
import br.com.zamfir.comprarei.view.listeners.PhotoSelectedListener
import br.com.zamfir.comprarei.view.listeners.PhotopickerListener
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
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

        val pickedMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri ->
            if(uri != null){
               PhotoSelectedListener.photoSelectedListener.onPhotoSelected(uri)
            }else{
                Log.d("DEBUG", "No media selected")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestNotificationPermission()
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

        PhotopickerListener.setOnListener(object : PhotopickerListener{
            override fun onPhotoClicked() {
               pickedMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permissao_notificaco_negada))
                        .setMessage(getString(R.string.msg_permissao_notificacao_negada))
                        .setPositiveButton(getString(R.string.ok)){ dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }


        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {}

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS) -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permissao_notificacao))
                    .setMessage(getString(R.string.msg_permissao_notificacao))
                    .setPositiveButton(getString(R.string.conceder)){ dialog, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.nao_conceder)){ dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            else -> {
                Toast.makeText(this, "Permissão de notificação não concedida.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBackupWorker() {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val periodicWorker = PeriodicWorkRequestBuilder<BackupWorker>(
            6,
            TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Backup_Unique_Job",
            ExistingPeriodicWorkPolicy.KEEP, periodicWorker
        )
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}