package br.com.zamfir.comprarei.mylibrary.telegram.repository

import android.util.Log
import br.com.zamfir.comprarei.mylibrary.telegram.api.TelegramRetrofit
import br.com.zamfir.comprarei.mylibrary.telegram.model.MessageBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TelegramRepository{

    companion object {
        fun doRequest(message : String) = CoroutineScope(Dispatchers.IO).launch{
            try{
                val body = MessageBody(text = message)
                val call = TelegramRetrofit.apiService.enviarNotificacao(body)
                val response = call?.execute()

                if(response?.isSuccessful == false){
                    Log.d("Telegram", " Request error - ${response.errorBody()?.string()}")
                }
            }catch(e : Exception) {
                e.printStackTrace()
            }
        }
    }

}