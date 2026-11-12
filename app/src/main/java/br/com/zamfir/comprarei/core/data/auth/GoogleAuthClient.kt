package br.com.zamfir.comprarei.core.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.util.log.AppLogger
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

/** Resultado do pedido de conta Google pelo Credential Manager. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult

    /** Usuário fechou a folha de contas. */
    data object Cancelled : GoogleSignInResult

    /** Nenhuma conta Google disponível no aparelho. */
    data object NoAccount : GoogleSignInResult
    data class Failure(val error: Exception) : GoogleSignInResult
}

/**
 * Pede uma conta Google ao sistema (Credential Manager), substituindo o One Tap depreciado.
 * Precisa do contexto da Activity, porque o sistema mostra uma folha de contas sobre ela.
 */
class GoogleAuthClient {

    suspend fun requestIdToken(activityContext: Context): GoogleSignInResult {
        // "Sign in with Google" para o botão explícito: mostra todas as contas do aparelho.
        val option = GetSignInWithGoogleOption.Builder(activityContext.getString(R.string.client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Failure(IllegalStateException("Credencial inesperada: ${credential.type}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            // O Play Services devolve "cancelado" com "[16] Account reauth failed" quando a conta é escolhida,
            // mas o app não bate com um cliente OAuth Android (SHA-1 não cadastrado). Não é cancelamento do usuário.
            if (e.message?.contains("[16]") == true) {
                AppLogger.error("Login Google recusado pelo Play Services (verifique o SHA-1 no Firebase)", e)
                GoogleSignInResult.Failure(e)
            } else {
                GoogleSignInResult.Cancelled
            }
        } catch (e: NoCredentialException) {
            GoogleSignInResult.NoAccount
        } catch (e: GetCredentialException) {
            AppLogger.error("Falha ao obter credencial Google", e)
            GoogleSignInResult.Failure(e)
        } catch (e: Exception) {
            AppLogger.error("Falha ao ler credencial Google", e)
            GoogleSignInResult.Failure(e)
        }
    }
}
