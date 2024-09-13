package br.com.zamfir.comprarei.util.exceptions

class InvalidLogin(val msg : String) : Exception(msg)
class InvalidEmail(val msg : String) : Exception(msg)
class InvalidPassword(val msg : String) : Exception(msg)
class InternalError(val msg : String) : Exception(msg)
class UserAlreadyExists(val msg : String) : Exception(msg)
class UserProfilePictureException(val msg : String) : Exception(msg)
class NoUserLogged(val msg : String) : Exception(msg)
class UserInfoPersistenceException(val msg : String) : Exception(msg)
class FirestoreLoadRegistersException(val msg : String) : Exception(msg)
class FirestoreDocumentCreationException(val msg : String) : Exception(msg)