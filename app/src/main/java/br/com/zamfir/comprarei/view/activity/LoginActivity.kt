package br.com.zamfir.comprarei.view.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.databinding.LoginActivityBinding
import br.com.zamfir.comprarei.util.GoogleSigninUtil
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.view.listeners.PhotoSelectedListener
import br.com.zamfir.comprarei.view.listeners.PhotopickerListener
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private var _binding: LoginActivityBinding? = null
    private val binding: LoginActivityBinding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    lateinit var activityResult: ActivityResultLauncher<IntentSenderRequest>

    lateinit var googleSigninUtil : GoogleSigninUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = LoginActivityBinding.inflate(layoutInflater)
        auth = Firebase.auth

        googleSigninUtil = GoogleSigninUtil(this)

        setContentView(binding.root)

        val pickedMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri ->
            if(uri != null){
                PhotoSelectedListener.photoSelectedListener.onPhotoSelected(uri)
            }else{
                Log.d("DEBUG", "No media selected")
            }
        }

        activityResult =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
                googleSigninUtil.doLogin(result)
            }

        PhotopickerListener.setOnListener(object : PhotopickerListener {
            override fun onPhotoClicked() {
                pickedMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        })
    }
}