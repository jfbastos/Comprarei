package br.com.zamfir.comprarei.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import br.com.zamfir.comprarei.core.util.log.AppLogger
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.firestore.BackupAbortedException
import br.com.zamfir.comprarei.core.data.firestore.BackupOutcome
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.util.Constants
import kotlinx.coroutines.CancellationException
import org.koin.java.KoinJavaComponent.inject

/**
 * Não mover este arquivo de pacote: o WorkManager persiste o nome completo da classe
 * e o job periódico é agendado com ExistingPeriodicWorkPolicy.KEEP. Se a classe mudar
 * de pacote, o job já agendado nos aparelhos dos usuários falharia com ClassNotFoundException.
 */
class BackupWorker(context : Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val firestoreRepository : FirestoreRepository by inject(FirestoreRepository::class.java)
    private val configRepository : ConfigRepository by inject(ConfigRepository::class.java)

    override suspend fun doWork(): Result {

        val isManualBackup = inputData.getBoolean(Constants.MANUAL_BACKUP_KEY, false)

        if(!configRepository.getIsAutoBackupEnabled() && !isManualBackup) return Result.success()

        return try {
            when (firestoreRepository.backup()) {
                BackupOutcome.Done -> {
                    if (isManualBackup) sendNotification("Backup", "Compras salvas com sucesso.")
                    Result.success()
                }
                // Sem usuário ou sem restauração concluída não há o que enviar; o manual avisa a tela.
                BackupOutcome.NoUser, BackupOutcome.NotRestored -> if (isManualBackup) Result.failure() else Result.success()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupAbortedException) {
            AppLogger.warning("Backup interrompido: ${e.message}")
            Result.failure()
        } catch (e: Exception) {
            AppLogger.error(if (isManualBackup) "Falha no backup manual" else "Falha no backup automático", e)
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