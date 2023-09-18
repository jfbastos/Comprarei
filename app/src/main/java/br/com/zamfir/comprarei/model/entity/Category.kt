package br.com.zamfir.comprarei.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "categories")
data class Category(
    var description : String,
    var color : Int
) : Serializable{
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}