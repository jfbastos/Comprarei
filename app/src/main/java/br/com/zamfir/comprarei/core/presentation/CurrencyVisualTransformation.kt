package br.com.zamfir.comprarei.core.presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import br.com.zamfir.comprarei.core.util.formatCents

/**
 * Exibe um campo que guarda só os dígitos dos centavos ("1250") como valor monetário ("R$ 12,50").
 * O número é preenchido da direita para a esquerda e o cursor fica sempre no fim.
 */
class CurrencyVisualTransformation(private val currencySymbol: String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formatted = formatCents(text.text.toLongOrNull() ?: 0L, currencySymbol)
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = formatted.length
                override fun transformedToOriginal(offset: Int) = text.length
            },
        )
    }

    override fun equals(other: Any?) = other is CurrencyVisualTransformation && other.currencySymbol == currencySymbol

    override fun hashCode() = currencySymbol.hashCode()

    companion object {
        /** Máximo de dígitos aceitos (até 9.999.999,99). */
        const val MAX_DIGITS = 9

        /** Mantém só dígitos, sem zeros à esquerda, limitado a [MAX_DIGITS]. */
        fun sanitize(input: String): String = input.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)
    }
}
