package br.com.zamfir.comprarei.util.log

import android.util.Log
import br.com.zamfir.comprarei.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Logger fino sobre o Crashlytics.
 * - [error]: registra uma exceção não fatal no Crashlytics.
 * - [warning]/[debug]: viram breadcrumbs (aparecem junto do próximo crash/erro registrado).
 * Em builds de debug tudo também vai para o Logcat.
 */
object AppLogger {

    private const val TAG = "Comprarei"

    fun error(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(TAG, message, throwable)
        crashlytics()?.run {
            log("E/$message")
            recordException(throwable ?: Exception(message))
        }
    }

    fun warning(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
        crashlytics()?.log("W/$message")
    }

    fun debug(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
        crashlytics()?.log("D/$message")
    }

    // O Firebase só é inicializado no processo principal; evita crash em testes ou outros processos.
    private fun crashlytics(): FirebaseCrashlytics? =
        runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
}
