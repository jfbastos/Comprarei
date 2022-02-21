package br.com.iesb.comprarei.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carts")
data class Cart(
    @PrimaryKey val id: String,
    val name: String,
    val data: String,
    val total : String
): java.io.Serializable