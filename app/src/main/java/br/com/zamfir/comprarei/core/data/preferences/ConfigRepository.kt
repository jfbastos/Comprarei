package br.com.zamfir.comprarei.core.data.preferences

import android.content.Context
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.util.DateUtil
import kotlinx.coroutines.CoroutineDispatcher
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

}