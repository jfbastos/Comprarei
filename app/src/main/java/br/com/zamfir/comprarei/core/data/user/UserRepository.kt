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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Evita que duas telas recuperem ou troquem a foto ao mesmo tempo.
    private val pictureMutex = Mutex()
    private var legacyCacheCleaned = false

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

    /** Foto de perfil para exibir: o arquivo local, recuperado se preciso (ver [ensureLocalProfilePicture]). */
    suspend fun getUserProfilePicture(): Uri? = ensureLocalProfilePicture()?.toUri()

    /**
     * Garante que a foto salva em [UserInfo.profilePicture] existe em [profilePictureDir] e devolve o arquivo.
     *
     * - Caminho vazio: o usuário não tem foto (não consulta a nuvem).
     * - Arquivo fora da pasta da foto (versões antigas gravavam no cache): move para a pasta.
     * - Arquivo apagado ou vazio: baixa de novo. Sem rede, devolve nulo e mantém o caminho para
     *   tentar depois; se a foto não existe mais na nuvem, limpa o caminho.
     *
     * Nunca lança exceção (só cancelamento): a foto é acessória e não deve virar erro na tela.
     */
    suspend fun ensureLocalProfilePicture(): File? = withContext(dispatcher) {
        pictureMutex.withLock {
            try {
                val userInfoDao = appDatabase.UserInfoDao()
                val path = userInfoDao.getUserInfo()?.profilePicture.orEmpty()
                if (path.isBlank()) {
                    deleteLegacyCachedPictures()
                    return@withLock null
                }

                val saved = File(path)
                // Versões antigas às vezes deixavam o arquivo vazio: conta como ausente.
                if (saved.length() > 0) {
                    if (saved.parentFile?.canonicalPath == profilePictureDir().canonicalPath) {
                        deleteLegacyCachedPictures()
                        return@withLock saved
                    }
                    val moved = newProfilePictureFile()
                    saved.copyTo(moved)
                    userInfoDao.updateProfilePicturePath(moved.absolutePath)
                    saved.delete()
                    deleteLegacyCachedPictures()
                    uploadLegacyProfilePicture(moved)
                    return@withLock moved
                }
                saved.delete()
                deleteLegacyCachedPictures()

                val downloaded = downloadProfilePicture()
                userInfoDao.updateProfilePicturePath(downloaded?.absolutePath.orEmpty())
                downloaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.warning("Foto de perfil local indisponível: ${e.message}")
                null
            }
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

    /**
     * Envia a foto e só depois troca a cópia local: se o envio falhar, a foto anterior continua.
     * A cópia local é gravada a partir dos mesmos bytes (não precisa baixar o que acabou de subir).
     */
    private suspend fun updateProfilePicture(data: ByteArray) = withContext(dispatcher){
        try{
            firestoreRepository.uploadProfilePicture(data)

            pictureMutex.withLock {
                val previous = appDatabase.UserInfoDao().getUserInfo()?.profilePicture.orEmpty()
                val localFile = newProfilePictureFile().apply { writeBytes(data) }
                saveUserInfo(localFile.absolutePath)
                if (previous.isNotBlank() && previous != localFile.absolutePath) File(previous).delete()
            }
        }catch (e : CancellationException){
            throw e
        }catch (e : Exception){
            AppLogger.error("Problem on profile picture persistence", e)
            throw UserProfilePictureException(context.getString(R.string.something_went_wrong_on_updating_profile_picture))
        }
    }

    /** Baixa a foto da nuvem no login. Sem foto ou com falha, devolve vazio (o login segue sem foto). */
    private suspend fun getProfilePicturePath(): String = withContext(dispatcher) {
        try {
            downloadProfilePicture()?.absolutePath.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.warning("Não foi possível baixar a foto de perfil: ${e.message}")
            Constants.EMPTY_STRING
        }
    }

    /**
     * Baixa a foto da nuvem para um arquivo novo em [profilePictureDir].
     * Devolve nulo se a conta não tem foto; outras falhas (rede, por exemplo) viram exceção.
     */
    private suspend fun downloadProfilePicture(): File? {
        val bytes = firestoreRepository.downloadProfilePicture() ?: return null
        return newProfilePictureFile().apply { writeBytes(bytes) }
    }

    /**
     * A foto de versões antigas ficava no Cloud Storage, que o projeto não acessa mais. Se ela ainda
     * está no aparelho e a nuvem não tem foto, envia para o Firestore para não se perder.
     * Melhor esforço: uma falha só é registrada.
     */
    private suspend fun uploadLegacyProfilePicture(file: File) {
        try {
            if (file.length() > LEGACY_UPLOAD_MAX_BYTES) {
                AppLogger.warning("Foto de perfil antiga grande demais para enviar: ${file.length()} bytes")
                return
            }
            if (firestoreRepository.downloadProfilePicture() == null) {
                firestoreRepository.uploadProfilePicture(file.readBytes())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.warning("Não foi possível enviar a foto de perfil antiga: ${e.message}")
        }
    }

    /**
     * Pasta da foto de perfil em filesDir. Não usar o cache: o Android pode limpá-lo a qualquer
     * momento, e a foto sumia até o próximo login.
     */
    private fun profilePictureDir(): File = File(context.filesDir, PROFILE_PICTURE_DIR).apply { mkdirs() }

    // Nome novo a cada foto: o Coil guarda imagens pelo caminho, e reaproveitar o nome mostraria a foto antiga.
    private fun newProfilePictureFile(): File = File(
        profilePictureDir(),
        "${Constants.PROFILE_PICTURE_DEFAULT_NAME}${System.currentTimeMillis()}${Constants.PROFILE_PICTURE_DEFAULT_EXTENSION}",
    )

    /** Apaga a foto local, inclusive a de versões antigas, que ficava no cache. */
    private fun deleteLocalProfilePictures() {
        appDatabase.UserInfoDao().getUserInfo()?.profilePicture?.takeIf { it.isNotBlank() }?.let { File(it).delete() }
        File(context.filesDir, PROFILE_PICTURE_DIR).deleteRecursively()
        legacyCacheCleaned = false
        deleteLegacyCachedPictures()
    }

    /**
     * Versões antigas criavam um arquivo no cache a cada foto (às vezes vazio) e não apagavam os
     * anteriores. Depois que a foto atual sai do cache, todos são lixo. Roda uma vez por processo.
     */
    private fun deleteLegacyCachedPictures() {
        if (legacyCacheCleaned) return
        context.cacheDir.listFiles { file -> file.isFile && file.name.startsWith(Constants.PROFILE_PICTURE_DEFAULT_NAME) }
            ?.forEach { it.delete() }
        legacyCacheCleaned = true
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
            deleteLocalProfilePictures()
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
     * Apaga a conta no Firebase e, só depois que deu certo, os dados locais. A foto na nuvem sai
     * junto com os outros dados em [FirestoreRepository.deleteUserData], chamado antes.
     * Lança exceção em qualquer falha (inclusive FirebaseAuthRecentLoginRequiredException se a
     * reautenticação expirou), deixando os dados do aparelho intactos.
     */
    suspend fun deleteUser() = withContext(dispatcher) {
        val currentUser = auth.currentUser ?: throw NoUserLogged(context.getString(R.string.no_user_logged))

        currentUser.delete().await()
        configRepository.clearSession()
        auth.signOut()
        deleteLocalProfilePictures()
        appDatabase.clearAllTables()
    }

    private companion object {
        const val PROFILE_PICTURE_DIR = "profile_picture"
        // Mesmo limite do documento no Firestore (fotos antigas não passavam pelo compressor).
        const val LEGACY_UPLOAD_MAX_BYTES = 900 * 1024
    }
}
