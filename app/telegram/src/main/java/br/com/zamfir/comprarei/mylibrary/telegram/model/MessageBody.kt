package br.com.zamfir.comprarei.mylibrary.telegram.model

import com.google.gson.annotations.SerializedName

data class MessageBody(
    @SerializedName("chat_id")
    val chatId : String = "-4789277713",
    val text : String = "",
    val parseMode : String = "Markdown"
)