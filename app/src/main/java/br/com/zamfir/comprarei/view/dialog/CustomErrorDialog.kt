package br.com.zamfir.comprarei.view.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import br.com.zamfir.comprarei.databinding.DialogCustomCrashBinding

class CustomErrorDialog() : DialogFragment() {

    private var _binding : DialogCustomCrashBinding? = null
    private val binding : DialogCustomCrashBinding get() = _binding!!

    private var errorDetails : String = ""

    constructor(errorDetail : String) : this(){
        this.errorDetails = errorDetail
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = DialogCustomCrashBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.errorBody.text = errorDetails

        binding.okBtn.setOnClickListener {
            dismiss()
        }

        return builder.create()
    }
}