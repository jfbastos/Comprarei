package br.com.zamfir.comprarei.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "CATEGORIES")
data class Category(
    var description : String,
    var color : Int,
    var firestoreUUID: String = UUID.randomUUID().toString()
) : Serializable{
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}