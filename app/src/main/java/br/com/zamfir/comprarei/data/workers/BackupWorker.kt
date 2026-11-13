package br.com.zamfir.comprarei.data.workers

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.firestore.BackupAbortedException
import br.com.zamfir.comprarei.core.data.firestore.BackupOutcome
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.util.log.AppLogger
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
                    if (isManualBackup) notifyManualBackupDone()
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

    /** Avisa que o backup manual terminou (ele pode concluir só quando a internet voltar, com o app fechado). */
    private fun notifyManualBackupDone() {
        val context = applicationContext
        val notificationManager = NotificationManagerCompat.from(context)
        // Sem a permissão (Android 13+) ou com notificações desligadas não há o que mostrar.
        if (!notificationManager.areNotificationsEnabled()) return

        // Mesmo id de canal das versões anteriores, para manter as preferências que o usuário já ajustou.
        // Som e vibração vêm do canal; recriar o canal só atualiza o nome (traduzido).
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.backup_notification_channel))
            .build()
        notificationManager.createNotificationChannel(channel)

        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_backup)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.backup_notification_title))
            .setContentText(context.getString(R.string.backup_notification_text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // A permissão pode ser revogada entre a checagem e o envio.
            AppLogger.warning("Notificação de backup não enviada: ${e.message}")
        }
    }

    private companion object {
        const val CHANNEL_ID = "99999"
        const val NOTIFICATION_ID = 99999
    }
}