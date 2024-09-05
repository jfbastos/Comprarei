package br.com.zamfir.comprarei.viewmodel.states

import br.com.zamfir.comprarei.model.entity.Product

data class ProductState (val products : List<Product>, val moveToBottom : Boolean)