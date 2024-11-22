package br.com.zamfir.comprarei.util.exceptions

class InvalidLogin(val msg : String) : Exception(msg)
class InvalidEmail(msg : String) : Exception(msg)
class InvalidPassword(val msg : String) : Exception(msg)
class InternalError(msg : String) : Exception(msg)
class UserAlreadyExists(val msg : String) : Exception(msg)
class UserProfilePictureException(msg : String) : Exception(msg)
class NoUserLogged(msg : String) : Exception(msg)
class UserInfoPersistenceException(msg : String) : Exception(msg)
class FirestoreLoadRegistersException(msg : String) : Exception(msg)
class FirestoreDocumentCreationException(msg : String) : Exception(msg)