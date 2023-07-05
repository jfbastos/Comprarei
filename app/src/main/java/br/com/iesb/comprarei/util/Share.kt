package br.com.iesb.comprarei.util

import android.content.Context
import android.content.Intent

class Share {
    fun send(text: String, context : Context) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}