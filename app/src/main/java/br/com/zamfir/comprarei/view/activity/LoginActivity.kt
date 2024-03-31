package br.com.zamfir.comprarei.view.activity

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.databinding.LoginActivityBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private var _binding : LoginActivityBinding? = null
    private val binding : LoginActivityBinding get() = _binding!!

    lateinit var oneTapClient: SignInClient
    lateinit var signUpRequest: BeginSignInRequest

    private lateinit var auth: FirebaseAuth

    lateinit var activityResult : ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){ result : ActivityResult ->
            if(result.resultCode == Activity.RESULT_OK){
                try {
                    val googleCredential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = googleCredential.googleIdToken
                    when {
                        idToken != null -> {
                            val idToken = googleCredential.googleIdToken
                            when {
                                idToken != null -> {
                                    // Got an ID token from Google. Use it to authenticate
                                    // with Firebase.
                                    val firebaseCredential =
                                        GoogleAuthProvider.getCredential(idToken, null)
                                    auth.signInWithCredential(firebaseCredential)
                                        .addOnCompleteListener(this) { task ->
                                            if (task.isSuccessful) {
                                                // Sign in success, update UI with the signed-in user's information
                                                Log.d("DEBUG", "signInWithCredential:success")
                                                val user =
                                                    Log.d("DEBUG", "Token UUID = ${auth.currentUser?.uid} | ${auth.currentUser?.displayName} | ${auth.currentUser?.email}")
                                            } else {
                                                // If sign in fails, display a message to the user.
                                                Log.w(
                                                    "DEBUG",
                                                    "signInWithCredential:failure",
                                                    task.exception
                                                )
                                            }
                                        }
                                }

                                else -> {
                                    // Shouldn't happen.
                                    Log.d("DEBUG", "No ID token!")
                                }
                            }
                            Log.d("DEBUG", "Got ID token.")
                        }

                        else -> {
                            // Shouldn't happen.
                            Log.d("DEBUG", "No ID token!")
                        }
                    }
                } catch (e: ApiException) {
                    Log.d("DEBUG", e.stackTraceToString())
                }
            }
        }

        auth = Firebase.auth

        oneTapClient = Identity.getSignInClient(this)
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId("24013763873-mu1pqttcq64v4ua67vh3jminj916o65r.apps.googleusercontent.com")
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }
}