package br.com.zamfir.comprarei.util.log

import android.os.Build
import br.com.zamfir.comprarei.BuildConfig
import br.com.zamfir.comprarei.mylibrary.telegram.TelegramLog
import java.util.Date

object LogUtil{

    fun sendLog(logLevel: TelegramLogLevel, message: String){
        when(logLevel){
            TelegramLogLevel.DEBUG -> TelegramLog.sendLog("`LEVEL: DEBUG` \n\n $message ${defaultInfo()}", Date())
            TelegramLogLevel.WARNING -> TelegramLog.sendLog("`LEVEL: WARNING` \n\n $message ${defaultInfo()}", Date())
            TelegramLogLevel.ERROR -> TelegramLog.sendLog("`LEVEL: ERROR` \n\n $message ${defaultInfo()}", Date())
            TelegramLogLevel.CRITICAL -> TelegramLog.sendLog("`LEVEL:CRITICAL` \n\n $message ${defaultInfo()}", Date())
        }
    }

    private fun defaultInfo(): String {
        return "\n\n*Android* ${Build.VERSION.RELEASE}\n*Modelo* : ${Build.MODEL}\n*Versão*: ${getVersion()}"
    }

    private fun getVersion() : String{
        return if(BuildConfig.DEBUG){
            "em Desenvolvimento"
        }else{
            BuildConfig.VERSION_NAME
        }
    }
}