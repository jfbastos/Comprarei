package br.com.zamfir.comprarei.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.repositories.FirestoreRepository
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

class BackupWorker(context : Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val firestoreRepository : FirestoreRepository by inject(FirestoreRepository::class.java)

    override suspend fun doWork(): Result {
        return try{
            firestoreRepository.saveCategories()
            firestoreRepository.saveProducts()
            firestoreRepository.saveCategories()
            applicationContext.getSharedPreferences("shared", Context.MODE_PRIVATE).edit {
                putString("last_backup", LocalDateTime.now().toString())
            }
            sendNotification("Backup Comprarei", "Backup realizado com sucesso.")
            Result.success()
        }catch (e : Exception){
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendNotification(title : String, msg : String) {
        val channelName = "COMPRAREI_CHANNEL"
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
            .setSmallIcon(R.mipmap.ic_launcher_comprarei) // Replace with your icon resource

        // Send notification
        notificationManager?.notify(notificationId, builder.build())
    }
}