package br.com.zamfir.comprarei.core.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.util.Locale

class MoneyTest {

    private val ptBr = Locale.forLanguageTag("pt-BR")
    private val enUs = Locale.US

    @Test
    fun `toCents rounds half a cent up`() {
        assertThat(12.345.toCents()).isEqualTo(1235L)
        assertThat(12.344.toCents()).isEqualTo(1234L)
    }

    @Test
    fun `toCents avoids binary floating point error`() {
        // 0.1 + 0.2 = 0.30000000000000004 e 1.005 é guardado como 1.00499999...
        assertThat((0.1 + 0.2).toCents()).isEqualTo(30L)
        assertThat(1.005.toCents()).isEqualTo(101L)
    }

    @Test
    fun `formats with portuguese separators`() {
        assertThat(formatCents(123456, "R$", ptBr)).isEqualTo("R$ 1.234,56")
    }

    @Test
    fun `formats with english separators`() {
        assertThat(formatCents(123456, "$", enUs)).isEqualTo("$ 1,234.56")
    }

    @Test
    fun `formats values above one million`() {
        assertThat(formatMoney(1_234_567.89, "R$", ptBr)).isEqualTo("R$ 1.234.567,89")
    }

    @Test
    fun `formats zero and small values`() {
        assertThat(formatCents(0, "R$", ptBr)).isEqualTo("R$ 0,00")
        assertThat(formatCents(5, "R$", ptBr)).isEqualTo("R$ 0,05")
    }
}
