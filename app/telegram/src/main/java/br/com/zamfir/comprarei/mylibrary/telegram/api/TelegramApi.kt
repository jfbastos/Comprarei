package br.com.zamfir.comprarei.mylibrary.telegram.api

import br.com.zamfir.comprarei.mylibrary.telegram.model.MessageBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TelegramApi {

    companion object{
        const val BASE_URL = "https://api.telegram.org/bot7442763116:AAEhT0d6Pu02YV1DIukkRZz_h0t-aHAOhfg/"
    }

    @POST("sendMessage")
    fun enviarNotificacao(
        @Body message: MessageBody?,
    ): Call<String>?
}