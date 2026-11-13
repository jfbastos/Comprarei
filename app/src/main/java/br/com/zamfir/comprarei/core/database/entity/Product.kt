package br.com.zamfir.comprarei.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Produto de uma compra. Imutável; o [id] fica por último pelo mesmo motivo explicado em [Cart]. */
@Entity(tableName = "PRODUCTS")
data class Product(
    val name: String,
    val brand: String = "",
    val price: Double,
    val quantity: Int,
    val cartId: Int,
    val done: Boolean = false,
    val position: Int,
    val firestoreUUID: String = UUID.randomUUID().toString(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
