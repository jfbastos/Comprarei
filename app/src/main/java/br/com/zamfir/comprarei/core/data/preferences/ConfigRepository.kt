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
import java.time.LocalDateTime

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

    /*
     * Restauração dos dados após o login. Enquanto não estiver concluída para o usuário logado,
     * o banco local pode estar vazio ou pela metade e não serve de referência para o backup
     * (o sync apaga da nuvem o que não existe no aparelho).
     */

    /** Marca o início da restauração para [uid]; uma restauração interrompida fica "iniciada" e não "concluída". */
    fun markRestoreStarted(uid: String) {
        sharedPref.edit(commit = true) {
            putString(RESTORE_STARTED_UID_KEY, uid)
            remove(RESTORE_COMPLETED_UID_KEY)
        }
    }

    /** Gravado com commit (síncrono): o backup só pode rodar depois que isto estiver no disco. */
    fun markRestoreCompleted(uid: String) {
        sharedPref.edit(commit = true) { putString(RESTORE_COMPLETED_UID_KEY, uid) }
    }

    fun isRestoreCompleted(uid: String): Boolean = sharedPref.getString(RESTORE_COMPLETED_UID_KEY, null) == uid

    fun wasRestoreStarted(uid: String): Boolean = sharedPref.getString(RESTORE_STARTED_UID_KEY, null) == uid

    /** Apaga o estado da sessão (restauração e último backup) ao sair ou excluir a conta. */
    fun clearSession() {
        sharedPref.edit(commit = true) {
            remove(RESTORE_STARTED_UID_KEY)
            remove(RESTORE_COMPLETED_UID_KEY)
            remove(context.getString(R.string.last_backup_key))
        }
    }

    fun setLastBackupTime(time: LocalDateTime) {
        sharedPref.edit { putString(context.getString(R.string.last_backup_key), time.toString()) }
    }

    private companion object {
        const val THEME_MODE_KEY = "theme_mode"
        const val NOTIFICATION_ASKED_KEY = "notification_permission_asked"
        const val RESTORE_STARTED_UID_KEY = "restore_started_uid"
        const val RESTORE_COMPLETED_UID_KEY = "restore_completed_uid"
    }
}
