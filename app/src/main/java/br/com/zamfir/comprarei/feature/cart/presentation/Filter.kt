package br.com.zamfir.comprarei.feature.cart.presentation

import br.com.zamfir.comprarei.core.database.entity.Category
import java.time.LocalDate

class Filter {
    var sortOption : String? = null
    var byValue : ByValue? = null
    var byDate : ByDate? = null
    var byCategory: ByCategory? = null
}

data class ByValue(
    val operator : String,
    val valueMin : Double,
    val valueMax : Double
)

data class ByDate(
    val startDate : LocalDate,
    val endDate : LocalDate
)

data class ByCategory(
    val category: Category
)

