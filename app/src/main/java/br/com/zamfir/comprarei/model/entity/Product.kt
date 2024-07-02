package br.com.zamfir.comprarei.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    var name : String,
    var brand: String = "",
    var price : Double,
    var quantity : Int,
    val cartId : Int,
    var done : Boolean = false,
    var position : Int,
    var firestoreUUID: String = UUID.randomUUID().toString()
): Serializable {
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}
