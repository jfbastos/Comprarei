package br.com.zamfir.comprarei.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.data.repositories.ConfigRepository
import br.com.zamfir.comprarei.data.repositories.FirestoreRepository
import br.com.zamfir.comprarei.util.Constants
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime


class BackupWorker(context : Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val firestoreRepository : FirestoreRepository by inject(FirestoreRepository::class.java)
    private val configRepository : ConfigRepository by inject(ConfigRepository::class.java)

    override suspend fun doWork(): Result {

        val isManualBackup = inputData.getBoolean(Constants.MANUAL_BACKUP_KEY, false)

        if(!configRepository.getIsAutoBackupEnabled() && !isManualBackup) return Result.success()

        return try{
            firestoreRepository.saveCarts()
            firestoreRepository.saveProducts()
            firestoreRepository.saveCategories()
            applicationContext.getSharedPreferences(applicationContext.getString(R.string.shared_file_name), Context.MODE_PRIVATE).edit {
                putString(applicationContext.getString(R.string.last_backup_key), LocalDateTime.now().toString())
            }
            if(isManualBackup) sendNotification("Backup", "Compras salvas com sucesso.")
            Result.success()
        }catch (e : Exception){
            Log.d("DEBUG", "Failed to backup. Detail: ${e.stackTraceToString()}")
            Result.failure()
        }
    }

    private fun sendNotification(title : String, msg : String) {
        val channelName = "comprarei"
        val channelId = "99999"
        val notificationId = 99999

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)

        // Notification builder with channel id
        val builder = NotificationCompat.Builder(applicationContext, channelId)

        // Set notification content
        builder.setContentTitle(title)
            .setContentText(msg)
            .setSmallIcon(R.drawable.icone_fundo_branco) // Replace with your icon resource
            .setVibrate(longArrayOf(1000, 1000))

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(alarmSound)

        // Send notification
        notificationManager?.notify(notificationId, builder.build())
    }
}