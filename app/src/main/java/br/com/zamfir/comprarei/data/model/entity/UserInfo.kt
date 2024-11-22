package br.com.zamfir.comprarei.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "USER_INFO")
data class UserInfo(
    var name : String = "",
    var email : String = "",
    var profilePicture : String = "",
    var lastUpdate : String = LocalDateTime.now().toString()
){
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}
