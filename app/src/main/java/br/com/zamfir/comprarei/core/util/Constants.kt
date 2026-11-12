package br.com.zamfir.comprarei.core.util

class Constants {
    companion object {

        const val DATABASE_NAME = "comprarei_db"
        const val IO_DISPATCHER = "IODispacher"
        const val SORT_TAG = "BOTTOMSHEET"
        const val EMPTY_CART_VALUE = "R$ 0,00"
        const val DATE_PATTERN = "dd/MM/yyyy"
        const val CART_ID_KEY = "cartId"
        const val CART_NAME_KEY = "cartName"
        const val EMPTY_STRING = ""
        const val MANUAL_BACKUP_KEY = "MANUAL_BACKUP"
        const val MANUAL_BACKUP_WORK_NAME = "manual_backup"
        // Nome já agendado nos aparelhos dos usuários: não mudar.
        const val PERIODIC_BACKUP_WORK_NAME = "Backup_Unique_Job"

        const val FIRESTORE_STORAGE_NAME = "profilePictures"
        const val FIRESTORE_PROFILE_PICTURE_NAME = "profilePicture.jpg"
        const val PROFILE_PICTURE_DEFAULT_NAME = "profilePhoto_"
        const val PROFILE_PICTURE_DEFAULT_EXTENSION = ".jpg"
        const val FIRESTORE_DOCUMENT_PATH = "user_data"
        const val FIRESTORE_CARTS_DOCUMENT_PATH = "Carts"
        const val FIRESTORE_PRODUCTS_DOCUMENT_PATH = "Products"
        const val FIRESTORE_CATEGORIES_DOCUMENT_PATH = "Categories"


    }
}