package br.com.zamfir.comprarei.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/** Dados do usuário logado guardados no aparelho. Imutável; o [id] fica por último (ver [Cart]). */
@Entity(tableName = "USER_INFO")
data class UserInfo(
    val name: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val lastUpdate: String = LocalDateTime.now().toString(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
