package br.com.zamfir.comprarei.data.model.mappers

import br.com.zamfir.comprarei.data.model.entity.Cart
import br.com.zamfir.comprarei.data.model.entity.Category
import br.com.zamfir.comprarei.data.model.entity.Product

object FirebaseMapper {

    fun cartDocumentToEntity(document: MutableMap<String, Any>?) : Cart {
        return Cart(
            name = document?.get("name") as? String ?: "",
            data = document?.get("data") as? String ?: "",
            total = document?.get("total") as? String ?: "",
            position = (document?.get("position") as? Long ?: 0).toInt(),
            categoryId = (document?.get("categoryId") as? Long ?: 0).toInt(),
            store = document?.get("store") as? String ?: "",
            firestoreUUID = document?.get("firestoreUUID") as? String ?: ""
        ).apply {
            id = (document?.get("id") as? Long ?: 0).toInt()
        }
    }

    fun productDocumentToEntity(document: MutableMap<String, Any>?) : Product {
        return Product(
            name = document?.get("name") as? String ?: "",
            brand = document?.get("brand") as? String ?: "",
            price = document?.get("price") as? Double ?: 0.0,
            quantity = (document?.get("quantity") as? Long ?: 1).toInt(),
            cartId = (document?.get("cartId") as? Long ?: 0).toInt(),
            done = document?.get("done") as? Boolean ?: false,
            position = (document?.get("position") as? Long ?: 0).toInt(),
            firestoreUUID = document?.get("firestoreUUID") as? String ?: ""
        ).apply {
            id = (document?.get("id") as? Long ?: 0).toInt()
        }
    }

    fun categoryDocumentToEntity(document : MutableMap<String, Any>?) : Category {
        return Category(
            description = document?.get("description") as? String ?: "",
            color = (document?.get("color") as? Long ?: 0).toInt(),
            firestoreUUID = document?.get("firestoreUUID") as? String ?: ""
        ).apply {
            id = (document?.get("id") as? Long ?: 0).toInt()
        }
    }

}