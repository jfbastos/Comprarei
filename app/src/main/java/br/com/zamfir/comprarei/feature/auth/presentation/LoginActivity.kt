package br.com.zamfir.comprarei.feature.auth.presentation

import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.core.presentation.listeners.PhotoSelectedListener
import br.com.zamfir.comprarei.core.presentation.listeners.PhotopickerListener
import br.com.zamfir.comprarei.core.util.log.AppLogger
import br.com.zamfir.comprarei.databinding.LoginActivityBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

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
                AppLogger.debug("Nenhuma foto selecionada")
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

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}