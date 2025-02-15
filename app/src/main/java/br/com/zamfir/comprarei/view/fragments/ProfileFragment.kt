package br.com.zamfir.comprarei.view.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentProfileBinding
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.view.activity.DeleteAccountActivity
import br.com.zamfir.comprarei.view.dialog.CustomErrorDialog
import br.com.zamfir.comprarei.view.listeners.PhotoSelectedListener
import br.com.zamfir.comprarei.view.listeners.PhotopickerListener
import br.com.zamfir.comprarei.viewmodel.ProfileViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding: FragmentProfileBinding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModel()

    private var editedProfilePicture : Boolean = false

    private var cachedUserPictureUri : Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.getProfileInfos()

        PhotoSelectedListener.setOnListener(object : PhotoSelectedListener{
            override fun onPhotoSelected(uri: Uri) {
                binding.profilePicture.setImageURI(uri)
                editedProfilePicture = true
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            voltar()
        }

        binding.newPassword.doAfterTextChanged {
            binding.passwordIndicatorsPlaceholder.isVisible(!it.isNullOrBlank())
        }

        binding.profilePicture.setOnClickListener {
            PhotopickerListener.photopickerListener.onPhotoClicked()
        }

        binding.btnDeleteAccount.setOnClickListener {
            requireActivity().startActivity(Intent(requireActivity(), DeleteAccountActivity::class.java))
        }

        binding.btnSalvar.setOnClickListener {
            showLoading(isLoading = true)
            var photoByte : ByteArray? = null

            if(editedProfilePicture){
                val bitmap = ( binding.profilePicture.drawable as BitmapDrawable).bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                photoByte = baos.toByteArray()
            }

            profileViewModel.saveInfos(
                photo = photoByte,
                profileName = binding.user.text.takeIf { it != null }?.toString() ?: Constants.EMPTY_STRING,
                currentPassword = binding.currentPassword.text.takeIf { it != null }?.toString() ?: Constants.EMPTY_STRING,
                newPassword = binding.newPassword.text.takeIf { it != null }?.toString() ?: Constants.EMPTY_STRING
            )

        }

        profileViewModel.successfullySave.observe(viewLifecycleOwner){ editProfileState ->
            if(editProfileState.success){
                Snackbar.make(requireView(), getString(R.string.profile_successfully_edited), Snackbar.LENGTH_LONG).show()
                voltar()
            }else{
                showLoading(isLoading = false)
                CustomErrorDialog(editProfileState.error?.stackTraceToString() ?: Constants.EMPTY_STRING).show(parentFragmentManager, Constants.EMPTY_STRING)
            }
        }

        profileViewModel.profileState.observe(viewLifecycleOwner){ profileState ->
            binding.user.setText(profileState.userName)
            loadProfilePicture(cachedUserPictureUri ?: profileState.profilePicture)
        }
    }

    private fun loadProfilePicture(imageUri: Uri?) {
        imageUri?.let {
            Glide.with(requireContext())
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.profilePicture)
        }
        binding.loadingProgress.isVisible(false)
        binding.loadingProfilePicture.isVisible(false)
    }

    private fun showLoading(isLoading : Boolean){
        binding.btnSalvar.visibility = if(isLoading) View.INVISIBLE else View.VISIBLE
        binding.loadingBtn.isVisible(isLoading)
    }

    private fun voltar() {
        findNavController().popBackStack(R.id.configuracaoFragment, false)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}