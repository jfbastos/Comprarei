package br.com.zamfir.comprarei.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.zamfir.comprarei.databinding.ActivityCrashBinding
import br.com.zamfir.comprarei.util.log.LogUtil
import br.com.zamfir.comprarei.util.log.TelegramLogLevel

class CrashActivity : AppCompatActivity() {

    var _binding : ActivityCrashBinding? = null
    val binding : ActivityCrashBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityCrashBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val sb = StringBuilder()

        sb.append(intent.getStringExtra("Software"))
        sb.append("\n")
        sb.append(intent.getStringExtra("Error"))
        sb.append("\n\n")
        sb.append(intent.getStringExtra("Date"))

        binding.errorBody.text = sb.toString()

        LogUtil.sendLog(TelegramLogLevel.ERROR, sb.toString())

        binding.restartApp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}