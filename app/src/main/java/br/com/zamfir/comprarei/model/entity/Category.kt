package br.com.zamfir.comprarei.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    val description : String,
    val color : Int
){
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}