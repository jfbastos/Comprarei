package br.com.zamfir.comprarei.core.data.preferences

import android.content.Context
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.util.DateUtil
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

class ConfigRepository(private val context : Context, private val dispatcher: CoroutineDispatcher) {

    private val sharedPref = context.getSharedPreferences(context.getString(R.string.shared_file_name), Context.MODE_PRIVATE)

    suspend fun toggleDoneItensToBottom(isEnable : Boolean) = withContext(dispatcher) {
        with (sharedPref.edit()) {
            putBoolean(context.getString(R.string.donetobottom_key), isEnable)
            apply()
        }
    }

    suspend fun toggleShowTotalCart(isEnable: Boolean) = withContext(dispatcher){
        with(sharedPref.edit()){
            putBoolean(context.getString(R.string.showtotalcart_key), isEnable)
            apply()
        }
    }

    fun toggleAutoBackup(isEnable: Boolean) {
        with(sharedPref.edit()){
            putBoolean(context.getString(R.string.auto_backup_key), isEnable)
            apply()
        }
    }

    suspend fun getLastBackupTime() = withContext(dispatcher){
        return@withContext DateUtil.formatDate(sharedPref.getString(context.getString(R.string.last_backup_key), "") ?: "")
    }

    suspend fun getIsToMoveToBottomDoneItens() = withContext(dispatcher){
        return@withContext sharedPref.getBoolean(context.getString(R.string.donetobottom_key), false)
    }

    suspend fun getIsToShowTotalCart() =  withContext(dispatcher){
        return@withContext sharedPref.getBoolean(context.getString(R.string.showtotalcart_key), true)
    }

    suspend fun getIsAutoBackupEnabled() = withContext(dispatcher){
        return@withContext sharedPref.getBoolean(context.getString(R.string.auto_backup_key), true)
    }

    /** Leitura direta, para a Activity já abrir no tema certo (sem piscar o tema padrão). */
    fun getThemeMode(): ThemeMode =
        sharedPref.getString(THEME_MODE_KEY, null)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.System

    /** Emite o tema atual e cada mudança feita em Configurações. */
    fun observeThemeMode(): Flow<ThemeMode> = callbackFlow {
        trySend(getThemeMode())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == THEME_MODE_KEY) trySend(getThemeMode())
        }
        sharedPref.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPref.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = withContext(dispatcher) {
        sharedPref.edit { putString(THEME_MODE_KEY, mode.name) }
    }

    /** A permissão de notificação é pedida uma vez só; depois, só pelas configurações do sistema. */
    fun wasNotificationPermissionAsked(): Boolean = sharedPref.getBoolean(NOTIFICATION_ASKED_KEY, false)

    fun setNotificationPermissionAsked() {
        sharedPref.edit { putBoolean(NOTIFICATION_ASKED_KEY, true) }
    }

    private companion object {
        const val THEME_MODE_KEY = "theme_mode"
        const val NOTIFICATION_ASKED_KEY = "notification_permission_asked"
    }
}
