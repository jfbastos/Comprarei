package br.com.zamfir.comprarei.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.data.repositories.FirestoreRepository
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

class BackupWorker(context : Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val firestoreRepository : FirestoreRepository by inject(FirestoreRepository::class.java)

    override suspend fun doWork(): Result {
        return try{
            firestoreRepository.saveCarts()
            firestoreRepository.saveProducts()
            firestoreRepository.saveCategories()
            applicationContext.getSharedPreferences(applicationContext.getString(R.string.shared_file_name), Context.MODE_PRIVATE).edit {
                putString(applicationContext.getString(R.string.last_backup_key), LocalDateTime.now().toString())
            }
            sendNotification("Backup", "Compras salvas com sucesso.")
            Result.success()
        }catch (e : Exception){
            e.printStackTrace()
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

        // Send notification
        notificationManager?.notify(notificationId, builder.build())
    }
}