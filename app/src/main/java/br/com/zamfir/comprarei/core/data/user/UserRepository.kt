package br.com.zamfir.comprarei.core.data.user

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.data.firestore.FirestoreRepository
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.entity.UserInfo
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidEmail
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidLogin
import br.com.zamfir.comprarei.core.domain.exceptions.InvalidPassword
import br.com.zamfir.comprarei.core.domain.exceptions.NoUserLogged
import br.com.zamfir.comprarei.core.domain.exceptions.UserAlreadyExists
import br.com.zamfir.comprarei.core.domain.exceptions.UserProfilePictureException
import br.com.zamfir.comprarei.core.util.log.AppLogger
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/** Troca de senha pedida no perfil. */
data class PasswordChange(val currentPassword: String, val newPassword: String)

class UserRepository(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val configRepository: ConfigRepository,
    private val firestoreRepository: FirestoreRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    private var auth : FirebaseAuth = Firebase.auth

    suspend fun hasUserLogged() = withContext(dispatcher){ auth.currentUser != null }

    suspend fun getUserFromDb() = withContext(dispatcher) { appDatabase.UserInfoDao().getUserInfo() }

    suspend fun requestPasswordReset(email : String) { withContext(dispatcher){ auth.sendPasswordResetEmail(email).await() } }

    suspend fun loginUser(email : String, password : String) = withContext(dispatcher){
        try{
            val currentUser = auth.signInWithEmailAndPassword(email, password).await().user

            saveUserInfo()

            return@withContext currentUser
        }catch (e : Exception){
            when(e){
                is FirebaseAuthInvalidCredentialsException -> throw InvalidLogin(context.getString(R.string.msg_exp_user_password_invalid))
                else -> throw e
            }
        }
    }

    suspend fun createNewUserInFirebase(email: String, user: String, password: String, photoByte: ByteArray?) = withContext(dispatcher) {
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
            // Conta nova: a nuvem está vazia, então o banco local já pode ser enviado no backup.
            firestoreRepository.markNewAccountReady()

            updateUserName(user)

            if (photoByte != null) {
                try {
                    updateProfilePicture(photoByte)
                } catch (e: Exception) {
                    // A conta já foi criada: salva os dados locais sem foto e avisa a tela, que segue para o app.
                    saveUserInfo()
                    throw UserProfilePictureException(context.getString(R.string.msg_exp_upload_profile_picture_failed))
                }
            } else {
                saveUserInfo()
            }

            return@withContext auth.currentUser

        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthUserCollisionException -> throw UserAlreadyExists(context.getString(R.string.user_already_exists))
                is FirebaseAuthWeakPasswordException -> throw InvalidPassword(context.getString(R.string.password_not_meet_minimum_requirement))
                // E-mail mal formado (validado antes na tela, mas o Firebase tem a palavra final).
                is FirebaseAuthInvalidCredentialsException -> throw InvalidEmail(context.getString(R.string.invalid_email))
                else -> throw e
            }
        }
    }

    /** Entra no Firebase com o token de uma conta Google (obtido pelo Credential Manager). */
    suspend fun loginWithGoogle(idToken: String) = withContext(dispatcher) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        saveUserInfo()
        auth.currentUser
    }

    suspend fun getUserName() : String = withContext(dispatcher){
        val firebaseUserName = auth.currentUser?.displayName ?: ""
        if(firebaseUserName.isNotBlank()) return@withContext firebaseUserName

        val currentUserName = appDatabase.UserInfoDao().getUserInfo()?.name
        if(!currentUserName.isNullOrBlank()) return@withContext currentUserName

        return@withContext ""
    }

    suspend fun getUserProfilePicture() = withContext(dispatcher){
        try{
            val userDb = getUserFromDb()

            if(userDb != null){
                val file = File(userDb.profilePicture)
                if(file.exists()) return@withContext file.toUri()
                else return@withContext null
            }

            return@withContext getProfilePictureUriFromFirestore()

        }catch (e : Exception){
            return@withContext null
        }
    }

    private suspend fun saveUserInfo(absolutePath: String = "") {
        try{
            val currenUserInfo = getUserFromDb()

            if(currenUserInfo != null){
                val updatedUser = UserInfo(
                    name = getUserName(),
                    lastUpdate = LocalDateTime.now().toString(),
                    email = currenUserInfo.email,
                    profilePicture = absolutePath.takeIf { it.isNotBlank() } ?: currenUserInfo.profilePicture
                ).apply {
                    id = currenUserInfo.id
                }

                appDatabase.UserInfoDao().save(updatedUser)
            }else{
                val newUser = UserInfo(
                    name = getUserName(),
                    lastUpdate = LocalDateTime.now().toString(),
                    email = auth.currentUser?.email ?: Constants.EMPTY_STRING,
                    profilePicture = getProfilePicturePath()
                )

                appDatabase.UserInfoDao().save(newUser)
            }
        }catch (e : Exception){
            AppLogger.error("Failed to persist user info image", e)
        }
    }



    /** E-mail da conta logada (Firebase, ou o salvo localmente se estiver offline). */
    suspend fun getUserEmail(): String = withContext(dispatcher) {
        auth.currentUser?.email ?: getUserFromDb()?.email ?: Constants.EMPTY_STRING
    }

    /** Se a conta entra com e-mail e senha (contas só Google não têm senha para alterar). */
    suspend fun hasPasswordLogin(): Boolean = withContext(dispatcher) {
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: false
    }

    /**
     * Atualiza só o que foi informado (nulo = sem alteração).
     * Erros de senha e de foto são relançados com mensagem traduzida para a tela mostrar.
     */
    suspend fun updateUser(userName: String?, passwordChange: PasswordChange?, photo: ByteArray?) = withContext(dispatcher){
        userName?.let { updateUserName(it) }

        passwordChange?.let { updatePassword(it.currentPassword, it.newPassword) }

        if (photo != null) {
            updateProfilePicture(photo)
        }else{
            saveUserInfo()
        }
    }

    private suspend fun updateUserName(userName: String) {
        val profileChange = userProfileChangeRequest {
            displayName = userName
        }

        auth.currentUser?.updateProfile(profileChange)?.await()
    }

    private suspend fun updatePassword(oldPassword : String, newPassword : String) = withContext(dispatcher){
        try{
            val userEmail = auth.currentUser?.email ?: getUserFromDb()?.email ?: ""
            val authProvider = EmailAuthProvider.getCredential(userEmail, oldPassword)

            auth.currentUser?.reauthenticate(authProvider)?.await()
            auth.currentUser?.updatePassword(newPassword)?.await()
        }catch (e : FirebaseAuthWeakPasswordException){
            // Precisa vir antes: é subclasse de FirebaseAuthInvalidCredentialsException.
            throw InvalidPassword(context.getString(R.string.password_not_meet_minimum_requirement))
        }catch (e : FirebaseAuthInvalidCredentialsException){
            throw InvalidPassword(context.getString(R.string.your_current_password_is_incorrect))
        }catch (e : Exception){
            AppLogger.error("Fail to update password", e)
            throw e
        }
    }

    private suspend fun updateProfilePicture(data: ByteArray) = withContext(dispatcher){
        try{
            appDatabase.UserInfoDao().getUserInfo()?.let {
                if (it.profilePicture.isNotBlank()) {
                    File(it.profilePicture).delete()
                }
            }

            val localFile = File.createTempFile(Constants.PROFILE_PICTURE_DEFAULT_NAME, Constants.PROFILE_PICTURE_DEFAULT_EXTENSION)

            val upload = getImageRef().putBytes(data).await()

            val downloadNewProfilePicture = getImageRef().getFile(localFile).await()

            if(upload.task.isSuccessful && downloadNewProfilePicture.task.isSuccessful) saveUserInfo(localFile.absolutePath)
            else throw IllegalStateException("Upload ou download da foto não concluído")
        }catch (e : Exception){
            AppLogger.error("Problem on profile picture persistence", e)
            throw UserProfilePictureException(context.getString(R.string.something_went_wrong_on_updating_profile_picture))
        }
    }

    private suspend fun getProfilePicturePath() = withContext(dispatcher){
        if(getProfilePictureUriFromFirestore() != null){
            val localFile = File.createTempFile(Constants.PROFILE_PICTURE_DEFAULT_NAME, Constants.PROFILE_PICTURE_DEFAULT_EXTENSION)

            getImageRef().getFile(localFile).await()

            if(localFile.exists()) return@withContext localFile.absolutePath
            else Constants.EMPTY_STRING
        } else {
            return@withContext Constants.EMPTY_STRING
        }
    }

    /**
     * Sai da conta e apaga os dados do aparelho. Quem chama decide antes se faz um último backup.
     * A marca de restauração some antes do banco ser limpo: um backup que esteja rodando
     * percebe e para, em vez de enviar o banco vazio (o que apagaria a nuvem).
     */
    suspend fun logOffUser() = withContext(dispatcher){
        return@withContext try{
            firestoreRepository.cancelBackups()
            configRepository.clearSession()
            auth.signOut()
            appDatabase.UserInfoDao().getUserInfo()?.profilePicture?.let { File(it).delete() }
            appDatabase.clearAllTables()
            true
        }catch (e : Exception){
            AppLogger.error("Falha ao deslogar o usuário", e)
            false
        }
    }

    /** Se a conta entra com Google (pode ter senha também). */
    suspend fun hasGoogleLogin(): Boolean = withContext(dispatcher) {
        auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } ?: false
    }

    /**
     * Confirma a identidade com a senha atual, exigida pelo Firebase antes de operações sensíveis.
     * Senha errada vira [InvalidLogin].
     */
    suspend fun reauthenticateWithPassword(password: String) = withContext(dispatcher) {
        val user = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))
        try {
            user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), password)).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw InvalidLogin(context.getString(R.string.msg_exp_user_password_invalid))
        }
    }

    /** Confirma a identidade com a conta Google. Outra conta Google lança FirebaseAuthInvalidUserException. */
    suspend fun reauthenticateWithGoogle(idToken: String) = withContext(dispatcher) {
        val user = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    /**
     * Apaga a foto e a conta no Firebase e, só depois que tudo deu certo, os dados locais.
     * Lança exceção em qualquer falha (inclusive FirebaseAuthRecentLoginRequiredException se a
     * reautenticação expirou), deixando os dados do aparelho intactos.
     */
    suspend fun deleteUser() = withContext(dispatcher) {
        val currentUser = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))

        try {
            getImageRef().delete().await()
        } catch (e: StorageException) {
            // Conta sem foto de perfil: nada a apagar.
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }

        currentUser.delete().await()
        configRepository.clearSession()
        auth.signOut()

        appDatabase.UserInfoDao().getUserInfo()?.let {
            if (it.profilePicture.isNotBlank()) {
                File(it.profilePicture).delete()
            }
        }

        appDatabase.clearAllTables()
    }

    private fun getImageRef(): StorageReference {
        val currentUser = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))
        val userRef = Firebase.storage.reference.child(Constants.FIRESTORE_STORAGE_NAME).child(currentUser.uid)

        return userRef.child(Constants.FIRESTORE_PROFILE_PICTURE_NAME)
    }

    private suspend fun getProfilePictureUriFromFirestore() : Uri?{
        return try{
            getImageRef().downloadUrl.await()
        }catch (e : Exception){
            null
        }
    }
}