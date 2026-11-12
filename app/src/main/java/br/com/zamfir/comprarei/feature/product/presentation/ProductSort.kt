package br.com.zamfir.comprarei.feature.product.presentation

import br.com.zamfir.comprarei.core.database.entity.Product
import java.text.Collator

/** Ordenações da lista de produtos. Só mudam a exibição; a ordem salva é a [Manual]. */
enum class ProductSort {
    /** Ordem salva (arrastar e soltar). Único modo em que dá para reordenar. */
    Manual,
    Name,
    HighestTotal,
    LowestTotal,
    Quantity,
    PendingFirst,
}

/**
 * Aplica busca e ordenação sobre a lista na ordem salva.
 *
 * @param moveDoneToBottom preferência "comprados no fim", respeitada na ordem manual.
 */
fun List<Product>.applySortAndSearch(
    sort: ProductSort,
    query: String,
    moveDoneToBottom: Boolean,
    collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY },
): List<Product> {
    val term = query.trim()
    val searched = if (term.isEmpty()) this else filter {
        it.name.contains(term, ignoreCase = true) || it.brand.contains(term, ignoreCase = true)
    }

    // sortedBy é estável: empates mantêm a ordem salva.
    return when (sort) {
        ProductSort.Manual -> if (moveDoneToBottom) searched.sortedBy { it.done } else searched
        ProductSort.Name -> searched.sortedWith(compareBy(collator) { it.name })
        ProductSort.HighestTotal -> searched.sortedByDescending { it.subtotal }
        ProductSort.LowestTotal -> searched.sortedBy { it.subtotal }
        ProductSort.Quantity -> searched.sortedByDescending { it.quantity }
        ProductSort.PendingFirst -> searched.sortedBy { it.done }
    }
}

val Product.subtotal: Double get() = price * quantity
