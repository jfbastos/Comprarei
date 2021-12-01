package br.com.iesb.comprarei.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id : String,
    val name : String,
    val price : Double,
    val quantity : Int
)