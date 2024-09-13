package br.com.zamfir.comprarei.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.DialogForgotPasswordBinding
import br.com.zamfir.comprarei.util.errorAnimation
import br.com.zamfir.comprarei.util.resetErrorAnimation
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ForgotPasswordDialog :  BottomSheetDialogFragment() {

    private var _binding : DialogForgotPasswordBinding? = null
    private val binding : DialogForgotPasswordBinding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogForgotPasswordBinding.inflate(layoutInflater)


        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.DialogStyle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSend.setOnClickListener {
            if(validateFields()){
                loginViewModel.forgotPassword(binding.user.text.toString())
                dismiss()
            }
        }
    }

    private fun validateFields() : Boolean{
        val emailRegex = Regex("""^((?!\.)[\w\-_.]*[^.])(@\w+)(\.\w+(\.\w+)?[^.\W])$""")

        if(!emailRegex.matches(binding.user.text.toString())){
            binding.user.errorAnimation(getString(R.string.invalid_email))
            binding.user.doOnTextChanged { _, _, _, _ ->
                binding.userLayout.resetErrorAnimation()
            }
            binding.userLayout.requestFocus()
            return false
        }

        return true
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}