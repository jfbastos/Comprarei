package br.com.iesb.comprarei.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products")
data class Product(
    var name : String,
    var brand: String = "",
    var price : Double,
    var quantity : Int,
    val cartId : Int,
    var done : Boolean = false,
    var position : Int
): Serializable {
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}
