package br.com.zamfir.comprarei.feature.product.presentation

import br.com.zamfir.comprarei.core.database.entity.Product

data class ProductState (val products : List<Product>, val moveToBottom : Boolean)