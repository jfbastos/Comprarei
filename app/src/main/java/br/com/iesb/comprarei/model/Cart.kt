package br.com.iesb.comprarei.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "carts")
data class Cart(
    var name: String,
    var data: String,
    val total : String
): Serializable {
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}