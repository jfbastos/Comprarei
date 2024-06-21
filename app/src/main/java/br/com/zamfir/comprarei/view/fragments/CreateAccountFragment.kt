package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentSiginBinding
import br.com.zamfir.comprarei.util.errorAnimation
import br.com.zamfir.comprarei.util.resetErrorAnimation
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.view.activity.MainActivity
import br.com.zamfir.comprarei.view.listeners.PhotoSelectedListener
import br.com.zamfir.comprarei.view.listeners.PhotopickerListener
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.ByteArrayOutputStream

class CreateAccountFragment : Fragment() {

    private var _binding : FragmentSiginBinding? = null
    private val binding : FragmentSiginBinding get() = _binding!!

    private val loginViewModel : LoginViewModel by viewModel()

    private var editedProfilePicture : Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSiginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        loginViewModel.loginState.observe(viewLifecycleOwner){ loginState ->
            if (loginState.loading) return@observe

            if(loginState.success && loginState.user != null){
                showLoading(false)
                requireActivity().startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
                return@observe
            }

            if(!loginState.success && loginState.msgError.isNullOrBlank()){
                showLoading(false)
                Snackbar
                    .make(requireView(),
                        getString(R.string.something_went_wrong), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.more)){
                        AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.more_info))
                            .setMessage(
                                getString(
                                    R.string.create_account_error_detail,
                                    loginState.error
                                ))
                            .setPositiveButton(R.string.ok){ dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    .show()
            }else{
                showLoading(false)
                Snackbar.make(requireView(), loginState.msgError.toString(), Snackbar.LENGTH_LONG).show()
            }
        }

        binding.profilePicture.setOnClickListener {
            PhotopickerListener.photopickerListener.onPhotoClicked()
        }

        binding.passwordFirst.doOnTextChanged { text, _, _, _ ->
            binding.passwordFirstLayout.resetErrorAnimation()
            updateUppercaseIndicator(text?.any { it.isUpperCase() } ?: false)
            updateNumberIndicator(text?.any { it.isDigit()} ?: false)
            updateCharLengthIndicator(text.toString().length >= 6)
        }

        PhotoSelectedListener.setOnListener(object : PhotoSelectedListener {
            override fun onPhotoSelected(uri: Uri) {
                binding.profilePicture.setImageURI(uri)
                editedProfilePicture = true
            }
        })

        binding.btnLogin.setOnClickListener {
            var photoByte : ByteArray? = null
            arguments = Bundle().apply {
                putString("USER_KEY", binding.user.text.toString())
                putString("PASSWORD_KEY", binding.passwordFirst.text.toString())
            }

            if(editedProfilePicture){
                binding.profilePicture.isDrawingCacheEnabled = true
                binding.profilePicture.buildDrawingCache()
                val bitmap = ( binding.profilePicture.drawable as BitmapDrawable).bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                photoByte = baos.toByteArray()
            }


            if(validateFields()){
                showLoading(true)
                loginViewModel.createUser(binding.email.text.toString(), binding.user.text.toString(), binding.passwordFirst.text.toString(), photoByte)
            }
        }
    }

    private fun updateUppercaseIndicator(isValid : Boolean){
        binding.imgErrUppercase.isVisible(!isValid)
        binding.imgOkUppercase.isVisible(isValid)
        binding.txvUppercase.setTextColor(if(isValid) AppCompatResources.getColorStateList(requireContext(), R.color.primary_green) else AppCompatResources.getColorStateList(requireContext(), R.color.delete_red))
    }

    private fun updateNumberIndicator(isValid: Boolean){
        binding.imgErrNumber.isVisible(!isValid)
        binding.imgOkNumber.isVisible(isValid)
        binding.txvNumber.setTextColor(if(isValid) AppCompatResources.getColorStateList(requireContext(), R.color.primary_green) else AppCompatResources.getColorStateList(requireContext(), R.color.delete_red))
    }

    private fun updateCharLengthIndicator(isValid: Boolean){
        binding.imgErrQntChar.isVisible(!isValid)
        binding.imgOkQntChar.isVisible(isValid)
        binding.txvQntChar.setTextColor(if(isValid) AppCompatResources.getColorStateList(requireContext(), R.color.primary_green) else AppCompatResources.getColorStateList(requireContext(), R.color.delete_red))
    }

    private fun showLoading(isLoading : Boolean){
        binding.btnLogin.visibility = if(isLoading) View.INVISIBLE else View.VISIBLE
        binding.loadingBtn.isVisible(isLoading)
    }

    private fun validateFields() : Boolean{
        val emailRegex = Regex("""^((?!\.)[\w\-_.]*[^.])(@\w+)(\.\w+(\.\w+)?[^.\W])$""")

        if(!emailRegex.matches(binding.email.text.toString())){
            binding.emailLayout.errorAnimation(getString(R.string.invalid_email))
            binding.email.doOnTextChanged { _, _, _, _ ->
                binding.emailLayout.resetErrorAnimation()
            }
            binding.emailLayout.requestFocus()
            return false
        }

        if(binding.user.text.isNullOrBlank()){
            binding.userLayout.errorAnimation(getString(R.string.invalid_name))
            binding.user.doOnTextChanged { _, _, _, _ ->
                binding.userLayout.resetErrorAnimation()
            }
            binding.userLayout.requestFocus()
            return false
        }

        if(binding.passwordFirst.text.toString().none { it.isDigit() } || binding.passwordFirst.text.toString().none { it.isUpperCase() } || binding.passwordFirst.text.toString().length < 6){
            binding.passwordFirstLayout.errorAnimation(getString(R.string.password_requisites), true)
            binding.passwordFirstLayout.requestFocus()
            return false
        }

        if(binding.passwordFirst.text.toString() != binding.passwordSecond.text.toString()){
            binding.passwordFirstLayout.errorAnimation(getString(R.string.not_same_password), true)
            binding.passwordSecondLayout.errorAnimation(getString(R.string.not_same_password), true)
            binding.passwordSecond.doOnTextChanged { _, _, _, _ ->
                binding.passwordSecondLayout.resetErrorAnimation()
            }
            binding.passwordFirstLayout.requestFocus()
            return false
        }

        return true
    }
}