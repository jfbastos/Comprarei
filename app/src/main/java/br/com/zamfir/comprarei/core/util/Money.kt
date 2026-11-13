package br.com.zamfir.comprarei.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/*
 * Dinheiro no app: preços são Double no banco, totais são calculados a partir dos produtos
 * e só viram texto na hora de exibir. Contas e comparações usam centavos (Long), que não têm
 * erro de arredondamento.
 */

/** Valor em centavos, arredondando meio centavo para cima (ex.: 12.345 -> 1235). */
fun Double.toCents(): Long =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).unscaledValue().toLong()

/**
 * Formata centavos com o símbolo da moeda e separadores do idioma do aparelho
 * (ex.: "R$ 1.234,56" em português, "$ 1,234.56" em inglês).
 */
fun formatCents(cents: Long, currencySymbol: String, locale: Locale = Locale.getDefault()): String =
    "$currencySymbol ${String.format(locale, "%,.2f", BigDecimal.valueOf(cents, 2))}"

fun formatMoney(value: Double, currencySymbol: String, locale: Locale = Locale.getDefault()): String =
    formatCents(value.toCents(), currencySymbol, locale)
