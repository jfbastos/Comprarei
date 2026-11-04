package br.com.zamfir.comprarei.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import br.com.zamfir.comprarei.view.activity.CrashActivity
import java.util.Calendar
import kotlin.system.exitProcess

/**
 * Abre a [CrashActivity] e depois delega ao handler que estava instalado antes
 * (o do Crashlytics), para que o crash continue sendo registrado no Firebase.
 */
class CrashHandler(
    private val context: Context,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val errorMessage = Log.getStackTraceString(exception)
            val softwareInfo = buildString {
                append("SDK: ").append(Build.VERSION.SDK_INT).append('\n')
                append("Release: ").append(Build.VERSION.RELEASE).append('\n')
                append("Incremental: ").append(Build.VERSION.INCREMENTAL).append('\n')
            }
            val dateInfo = "${Calendar.getInstance().time}\n"

            val intent = Intent(context, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("Error", errorMessage)
                putExtra("Software", softwareInfo)
                putExtra("Date", dateInfo)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e("CrashHandler", "Falha ao abrir a CrashActivity", e)
        }

        if (previousHandler != null) {
            previousHandler.uncaughtException(thread, exception)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(2)
        }
    }

    companion object {
        fun install(context: Context) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext, previous))
        }
    }
}
