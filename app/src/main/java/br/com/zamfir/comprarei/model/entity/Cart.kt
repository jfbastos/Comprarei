package br.com.zamfir.comprarei.model.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "CARTS")
data class Cart(
    var name: String,
    var data: String,
    val total : String,
    var position : Int,
    var categoryId : Int = 0,
    var store : String = "",
    var firestoreUUID: String = UUID.randomUUID().toString()
): Serializable{
    @PrimaryKey(autoGenerate = true) var id : Int = 0
    @Ignore
    @get:Exclude var category : Category? = null
}