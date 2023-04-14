package br.com.iesb.comprarei.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "carts")
data class Cart(
    @PrimaryKey val id: String,
    val name: String,
    val data: String,
    val total : String
): Serializable