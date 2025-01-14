package br.com.zamfir.comprarei.mylibrary.telegram

import br.com.zamfir.comprarei.mylibrary.telegram.repository.TelegramRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramLog {

    private const val DATA_FORMAT = "HH:mm:ss dd/MM/yyyy"
    private val locale = Locale("pt", "BR")
    private val sdf = SimpleDateFormat(DATA_FORMAT, locale)

    fun sendLog(mensagem : String, data : Date){
        TelegramRepository.doRequest(("$mensagem\n*Data*: ${sdf.format(data)}"))
    }
}