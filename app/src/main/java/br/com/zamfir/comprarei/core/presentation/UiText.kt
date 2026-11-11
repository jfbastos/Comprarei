package br.com.zamfir.comprarei.core.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Texto para a UI que pode vir pronto (ex.: mensagem já traduzida de uma exceção)
 * ou de um recurso de string, resolvido só na hora de exibir.
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class StringResource(@StringRes val id: Int, vararg val args: Any) : UiText

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id, *args)
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(id, *args)
    }
}
