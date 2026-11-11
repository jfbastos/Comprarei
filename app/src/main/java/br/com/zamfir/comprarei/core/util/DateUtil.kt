package br.com.zamfir.comprarei.core.util

import br.com.zamfir.comprarei.core.util.log.AppLogger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object  DateUtil {

    fun formatDate(date : String) : String{
        if(date.isBlank()) return ""
        return try{
            val localdate = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME)
            localdate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        }catch (e : Exception){
            AppLogger.error("Falha ao formatar a data \"$date\"", e)
            ""
        }
    }
}