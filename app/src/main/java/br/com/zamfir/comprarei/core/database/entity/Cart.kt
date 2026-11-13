package br.com.zamfir.comprarei.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Compra. Imutável: para alterar, use copy().
 *
 * A ordem dos parâmetros é a ordem das colunas no schema do Room (o [id] fica por último);
 * mudá-la altera o hash de identidade do banco, que deixaria de abrir sem migração.
 * Os nomes também são os campos dos documentos no Firestore.
 */
@Entity(tableName = "CARTS")
data class Cart(
    val name: String,
    val data: String,
    val total: String,
    val position: Int,
    val categoryId: Int = 0,
    val store: String = "",
    val firestoreUUID: String = UUID.randomUUID().toString(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
