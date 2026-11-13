package br.com.zamfir.comprarei.feature.product.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import br.com.zamfir.comprarei.core.database.entity.Product
import org.junit.jupiter.api.Test
import java.text.Collator
import java.util.Locale

class ProductSortTest {

    // Collator fixo para o teste não depender do idioma da máquina.
    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply { strength = Collator.PRIMARY }

    private fun product(
        id: Int,
        name: String = "Produto $id",
        brand: String = "",
        price: Double = 1.0,
        quantity: Int = 1,
        done: Boolean = false,
    ) = Product(name = name, brand = brand, price = price, quantity = quantity, cartId = 1, done = done, position = id)
        .apply { this.id = id }

    private fun List<Product>.sorted(sort: ProductSort, query: String = "", moveDoneToBottom: Boolean = false) =
        applySortAndSearch(sort, query, moveDoneToBottom, collator).map { it.id }

    @Test
    fun `manual keeps saved order`() {
        val list = listOf(product(3), product(1, done = true), product(2))

        assertThat(list.sorted(ProductSort.Manual)).containsExactly(3, 1, 2)
    }

    @Test
    fun `manual moves bought products to the end when preference is on`() {
        val list = listOf(product(1, done = true), product(2), product(3, done = true), product(4))

        assertThat(list.sorted(ProductSort.Manual, moveDoneToBottom = true)).containsExactly(2, 4, 1, 3)
    }

    @Test
    fun `name ignores case and accents`() {
        val list = listOf(product(1, name = "ovo"), product(2, name = "Óleo"), product(3, name = "arroz"), product(4, name = "Açúcar"))

        assertThat(list.sorted(ProductSort.Name)).containsExactly(4, 3, 2, 1)
    }

    @Test
    fun `highest total uses price times quantity`() {
        val list = listOf(
            product(1, price = 10.0, quantity = 1),  // 10
            product(2, price = 3.0, quantity = 5),   // 15
            product(3, price = 50.0, quantity = 0),  // 0
        )

        assertThat(list.sorted(ProductSort.HighestTotal)).containsExactly(2, 1, 3)
        assertThat(list.sorted(ProductSort.LowestTotal)).containsExactly(3, 1, 2)
    }

    @Test
    fun `ties keep saved order`() {
        val list = listOf(product(1, price = 2.0, quantity = 2), product(2, price = 4.0, quantity = 1), product(3, price = 1.0))

        assertThat(list.sorted(ProductSort.HighestTotal)).containsExactly(1, 2, 3)
        assertThat(list.sorted(ProductSort.LowestTotal)).containsExactly(3, 1, 2)
    }

    @Test
    fun `quantity sorts from most to least`() {
        val list = listOf(product(1, quantity = 2), product(2, quantity = 12), product(3, quantity = 1))

        assertThat(list.sorted(ProductSort.Quantity)).containsExactly(2, 1, 3)
    }

    @Test
    fun `pending first ignores moveDoneToBottom preference`() {
        val list = listOf(product(1, done = true), product(2), product(3, done = true), product(4))

        assertThat(list.sorted(ProductSort.PendingFirst)).containsExactly(2, 4, 1, 3)
        assertThat(list.sorted(ProductSort.PendingFirst, moveDoneToBottom = true)).containsExactly(2, 4, 1, 3)
    }

    @Test
    fun `search matches name or brand ignoring case and spaces`() {
        val list = listOf(
            product(1, name = "Leite integral", brand = "Itambé"),
            product(2, name = "Café", brand = "Pilão"),
            product(3, name = "Pão", brand = "leiteria"),
        )

        assertThat(list.sorted(ProductSort.Manual, query = "  LEITE ")).containsExactly(1, 3)
        assertThat(list.sorted(ProductSort.Manual, query = "pilão")).containsExactly(2)
    }

    @Test
    fun `search is combined with the chosen sort`() {
        val list = listOf(
            product(1, name = "Arroz tipo 1", price = 30.0),
            product(2, name = "Feijão", price = 9.0),
            product(3, name = "Arroz integral", price = 12.0),
        )

        assertThat(list.sorted(ProductSort.LowestTotal, query = "arroz")).containsExactly(3, 1)
    }

    @Test
    fun `search without match returns empty list`() {
        assertThat(listOf(product(1, name = "Arroz")).sorted(ProductSort.Manual, query = "xyz")).isEmpty()
    }

    @Test
    fun `subtotal is price times quantity`() {
        assertThat(product(1, price = 2.5, quantity = 4).subtotal).isEqualTo(10.0)
    }
}
