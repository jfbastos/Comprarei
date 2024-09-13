package br.com.zamfir.comprarei.util

class Constants {
    companion object {

        const val DATABASE_NAME = "comprarei_db"
        const val IO_DISPATCHER = "IODispacher"
        const val SORT_TAG = "BOTTOMSHEET"
        const val NEW_CART_KEY = "NewCart"
        const val EMPTY_CART_VALUE = "R$ 0,00"
        const val DATE_PATTERN = "dd/MM/yyyy"
        const val MIN_DATE_VALID = "01/01/1900"
        const val MAX_DATE_VALID = "31/12/2050"
        const val CART_ID_KEY = "cartId"
        const val CART_NAME_KEY = "cartName"
        const val USER_KEY = "USER_KEY"
        const val PASSWORD_KEY = "PASSWORD_KEY"
        const val EMPTY_STRING = ""

        const val FILTER_NAME = "Name"
        const val FILTER_DATE = "Date"
        const val FILTER_VALUE_HIGH = "Value_high"
        const val FILTER_VALUE_LOW = "Value_low"
        const val FILTER_CATEGORY = "Category"
        const val FILTER_QUANTITY = "Quantity"
        const val FILTER_DONE = "Done"
        const val FILTER_UNDONE = "Undone"

        const val OPERATOR_EQUAL = "Equal"
        const val OPERATOR_LESS_THAN = "Less"
        const val OPERATOR_GRATER_THAN = "Greater"
        const val OPERATOR_RANGE = "Range"

        const val SHARED_LOGIN_GOOGLE_KEY = "LOGIN_WITH_GOOGLE"

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