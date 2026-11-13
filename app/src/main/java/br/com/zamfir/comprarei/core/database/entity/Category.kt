package br.com.zamfir.comprarei.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Categoria de compra. Imutável; o [id] fica por último pelo mesmo motivo explicado em [Cart]. */
@Entity(tableName = "CATEGORIES")
data class Category(
    val description: String,
    val color: Int,
    val firestoreUUID: String = UUID.randomUUID().toString(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
