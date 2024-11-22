package br.com.zamfir.comprarei.data.repositories

import android.content.Context
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.DateUtil
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

    suspend fun loggedWithGoogle(isFromGoogle : Boolean) = withContext(dispatcher){
        with(sharedPref.edit()){
            putBoolean(Constants.SHARED_LOGIN_GOOGLE_KEY, isFromGoogle)
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

    suspend fun getIsUserLoggedFromGoogle() = withContext(dispatcher){
        return@withContext sharedPref.getBoolean(Constants.SHARED_LOGIN_GOOGLE_KEY, true)
    }
}