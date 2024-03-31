package br.com.zamfir.comprarei.view.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.LoginActivityBinding
import br.com.zamfir.comprarei.view.listeners.LoginWithGoogleListener
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private var _binding: LoginActivityBinding? = null
    private val binding: LoginActivityBinding get() = _binding!!

    lateinit var oneTapClient: SignInClient
    lateinit var signUpRequest: BeginSignInRequest

    private lateinit var auth: FirebaseAuth

    lateinit var activityResult: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = LoginActivityBinding.inflate(layoutInflater)
        auth = Firebase.auth

        setContentView(binding.root)

        if(auth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
        }


        activityResult =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    try {
                        val googleCredential = oneTapClient.getSignInCredentialFromIntent(result.data)
                        val idToken = googleCredential.googleIdToken
                        idToken?.let{ token ->
                            signInWithGoogle(token)
                        }
                    } catch (e: ApiException) {
                        Log.d("DEBUG", e.stackTraceToString())
                    }
                }
            }


        oneTapClient = Identity.getSignInClient(this)
        signUpRequest = BeginSignInRequest.builder()
                            .setGoogleIdTokenRequestOptions(getGoogleRequestToken())
                            .build()
    }

    private fun getGoogleRequestToken() =
        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
            .setSupported(true)
            .setServerClientId(getString(R.string.client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

    private fun signInWithGoogle(idToken: String?) {
        val firebaseCredential =
            GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    LoginWithGoogleListener.loginListener.userLoggedIn()
                } else {
                   LoginWithGoogleListener.loginListener.loginError(task.exception)
                }
            }
    }
}