package br.com.iesb.comprarei.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "products")
data class Product(
    var name : String,
    var brand: String = "",
    var price : Double,
    var quantity : Int,
    val cartId : String
): java.io.Serializable {
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}
