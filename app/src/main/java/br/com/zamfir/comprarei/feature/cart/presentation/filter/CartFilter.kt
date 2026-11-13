package br.com.zamfir.comprarei.feature.cart.presentation.filter

import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.util.Constants
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Ordenação e filtros da lista de compras da Home. Imutável: cada mudança gera uma cópia. */
data class CartFilter(
    val sort: CartSort? = null,
    val dateRange: DateRange? = null,
    val value: ValueFilter? = null,
    // Ids locais das categorias aceitas; vazio = todas.
    val categoryIds: Set<Int> = emptySet(),
) {
    val isActive: Boolean get() = this != CartFilter()
}

enum class CartSort { Name, Newest, Oldest, HighestValue, LowestValue, Category }

enum class ValueOperator { Equal, LessThan, GreaterThan, Between }

data class DateRange(val start: LocalDate, val end: LocalDate)

/** Valores em centavos; [maxCents] só vale para [ValueOperator.Between] e nulo = sem limite superior. */
data class ValueFilter(val operator: ValueOperator, val minCents: Long, val maxCents: Long? = null)

/**
 * @param totalCents valor total de cada compra em centavos, por id local; compra ausente vale 0
 *   (compra sem produtos).
 */
fun List<Cart>.applyFilter(
    filter: CartFilter,
    totalCents: Map<Int, Long>,
    categories: Map<Int, Category>,
): List<Cart> {
    fun Cart.totalCents(): Long = totalCents[id] ?: 0L

    var result = this

    filter.dateRange?.let { range ->
        result = result.filter { cart ->
            val date = cart.parsedDate() ?: return@filter false
            !date.isBefore(range.start) && !date.isAfter(range.end)
        }
    }

    filter.value?.let { value ->
        result = result.filter { cart -> value.matches(cart.totalCents()) }
    }

    if (filter.categoryIds.isNotEmpty()) {
        result = result.filter { it.categoryId in filter.categoryIds }
    }

    return when (filter.sort) {
        null -> result
        CartSort.Name -> result.sortedBy { it.name.lowercase() }
        // Datas inválidas ficam no fim nos dois sentidos.
        CartSort.Newest -> result.sortedWith(compareBy(nullsLast(reverseOrder())) { it.parsedDate() })
        CartSort.Oldest -> result.sortedWith(compareBy(nullsLast()) { it.parsedDate() })
        CartSort.HighestValue -> result.sortedByDescending { it.totalCents() }
        CartSort.LowestValue -> result.sortedBy { it.totalCents() }
        CartSort.Category -> result.sortedWith(compareBy(nullsLast()) { categories[it.categoryId]?.description?.lowercase() })
    }
}

private fun ValueFilter.matches(cents: Long): Boolean = when (operator) {
    ValueOperator.Equal -> cents == minCents
    ValueOperator.LessThan -> cents < minCents
    ValueOperator.GreaterThan -> cents > minCents
    ValueOperator.Between -> {
        val upper = maxCents ?: Long.MAX_VALUE
        cents in minOf(minCents, upper)..maxOf(minCents, upper)
    }
}

private val CART_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(Constants.DATE_PATTERN)

private fun Cart.parsedDate(): LocalDate? = runCatching { LocalDate.parse(data, CART_DATE_FORMATTER) }.getOrNull()
