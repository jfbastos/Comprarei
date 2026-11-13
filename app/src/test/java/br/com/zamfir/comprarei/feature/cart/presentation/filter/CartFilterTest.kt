package br.com.zamfir.comprarei.feature.cart.presentation.filter

import assertk.assertThat
import assertk.assertions.containsExactly
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CartFilterTest {

    private fun cart(id: Int, name: String = "Compra $id", date: String = "10/11/2026", categoryId: Int = 0) =
        Cart(name = name, data = date, total = "", position = id, categoryId = categoryId, id = id)

    // O texto salvo em "total" é ignorado: o valor vem do mapa (calculado dos produtos).
    private val carts = listOf(cart(1), cart(2), cart(3))
    private val totals = mapOf(1 to 1_000L, 2 to 250_000_00L, 3 to 5_00L)

    private fun List<Cart>.ids() = map { it.id }

    @Test
    fun `no filter keeps saved order`() {
        assertThat(carts.applyFilter(CartFilter(), totals, emptyMap()).ids()).containsExactly(1, 2, 3)
    }

    @Test
    fun `value greater than uses computed totals`() {
        val filter = CartFilter(value = ValueFilter(ValueOperator.GreaterThan, minCents = 9_99))

        assertThat(carts.applyFilter(filter, totals, emptyMap()).ids()).containsExactly(1, 2)
    }

    @Test
    fun `value equal matches exact cents`() {
        val filter = CartFilter(value = ValueFilter(ValueOperator.Equal, minCents = 10_00))

        assertThat(carts.applyFilter(filter, totals, emptyMap()).ids()).containsExactly(1)
    }

    @Test
    fun `value between accepts limits in any order`() {
        val filter = CartFilter(value = ValueFilter(ValueOperator.Between, minCents = 20_00, maxCents = 5_00))

        assertThat(carts.applyFilter(filter, totals, emptyMap()).ids()).containsExactly(1, 3)
    }

    @Test
    fun `cart without products counts as zero`() {
        val filter = CartFilter(value = ValueFilter(ValueOperator.LessThan, minCents = 1))

        assertThat(listOf(cart(9)).applyFilter(filter, totals, emptyMap()).ids()).containsExactly(9)
    }

    @Test
    fun `sorts by highest and lowest value`() {
        assertThat(carts.applyFilter(CartFilter(sort = CartSort.HighestValue), totals, emptyMap()).ids()).containsExactly(2, 1, 3)
        assertThat(carts.applyFilter(CartFilter(sort = CartSort.LowestValue), totals, emptyMap()).ids()).containsExactly(3, 1, 2)
    }

    @Test
    fun `date range is inclusive and skips invalid dates`() {
        val list = listOf(cart(1, date = "01/11/2026"), cart(2, date = "30/11/2026"), cart(3, date = "01/12/2026"), cart(4, date = "x"))
        val filter = CartFilter(dateRange = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))

        assertThat(list.applyFilter(filter, emptyMap(), emptyMap()).ids()).containsExactly(1, 2)
    }

    @Test
    fun `newest first leaves invalid dates last`() {
        val list = listOf(cart(1, date = "x"), cart(2, date = "01/11/2026"), cart(3, date = "05/11/2026"))

        assertThat(list.applyFilter(CartFilter(sort = CartSort.Newest), emptyMap(), emptyMap()).ids()).containsExactly(3, 2, 1)
    }

    @Test
    fun `category filter keeps only selected categories`() {
        val list = listOf(cart(1, categoryId = 1), cart(2, categoryId = 2), cart(3))

        assertThat(list.applyFilter(CartFilter(categoryIds = setOf(2)), emptyMap(), emptyMap()).ids()).containsExactly(2)
    }

    @Test
    fun `sort by name ignores case`() {
        val list = listOf(cart(1, name = "feira"), cart(2, name = "Atacado"), cart(3, name = "mercado"))

        assertThat(list.applyFilter(CartFilter(sort = CartSort.Name), emptyMap(), emptyMap()).ids()).containsExactly(2, 1, 3)
    }

    @Test
    fun `sort by category puts carts without category last`() {
        val list = listOf(cart(1), cart(2, categoryId = 20), cart(3, categoryId = 30))
        val categories = listOf(Category("Obra", 0, id = 20), Category("casa", 0, id = 30)).associateBy { it.id }

        assertThat(list.applyFilter(CartFilter(sort = CartSort.Category), emptyMap(), categories).ids()).containsExactly(3, 2, 1)
    }
}
