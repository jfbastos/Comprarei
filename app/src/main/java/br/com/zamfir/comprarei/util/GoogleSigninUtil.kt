package br.com.zamfir.comprarei.util

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResult
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.view.listeners.LoginWithGoogleListener
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GoogleSigninUtil(private val callingActivity : Activity) {

    var oneTapClient: SignInClient = Identity.getSignInClient(callingActivity)
    var signUpRequest: BeginSignInRequest
    private var auth: FirebaseAuth

    init {
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(getGoogleRequestToken())
            .build()

        auth = Firebase.auth
    }

    fun doLogin(result: ActivityResult) {
        try {
            if(result.resultCode == Activity.RESULT_CANCELED){
                LoginWithGoogleListener.loginListener.userCancelled()
                return
            }

            val googleCredential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = googleCredential.googleIdToken
            idToken?.let{ token ->
                signInWithGoogle(token)
            }
        } catch (e: ApiException) {
            Log.d("DEBUG", e.stackTraceToString())
        }
    }

    private fun signInWithGoogle(idToken: String?) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(callingActivity) { task ->
                if (task.isSuccessful) {
                    LoginWithGoogleListener.loginListener.userLoggedIn()
                } else {
                    LoginWithGoogleListener.loginListener.loginError(task.exception)
                }
            }
    }

    private fun getGoogleRequestToken() =
        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
            .setSupported(true)
            .setServerClientId(callingActivity.getString(R.string.client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

}