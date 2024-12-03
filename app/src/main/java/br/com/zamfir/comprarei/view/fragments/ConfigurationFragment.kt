package br.com.zamfir.comprarei.view.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentConfigurationBinding
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.isVisible
import br.com.zamfir.comprarei.view.activity.LoginActivity
import br.com.zamfir.comprarei.viewmodel.ConfigViewModel
import br.com.zamfir.comprarei.viewmodel.LoginViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigurationFragment : Fragment() {

    private var _binding : FragmentConfigurationBinding? = null
    private val binding : FragmentConfigurationBinding get() = _binding!!

    private val loginViewModel : LoginViewModel by viewModel()
    private val configViewModel : ConfigViewModel by viewModel()

    private var userPictureUri : Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigurationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configViewModel.getConfigData()

        binding.toolbar.setNavigationOnClickListener {
            navigateBack()
        }

        binding.btnSair.setOnClickListener {
            loginViewModel.logOff()
        }

        binding.toggleProductsDone.setOnCheckedChangeListener { _, isChecked ->
            configViewModel.toggleProductsToBottom(isChecked)
        }

        binding.toggleCartTotal.setOnCheckedChangeListener { _, isChecked ->
            configViewModel.toggleShowCartTotal(isChecked)
        }

        binding.toggleAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            configViewModel.toggleAutoBackup(isChecked)
        }

        binding.btnDoBackup.setOnClickListener {
            configViewModel.doBackup()
        }

        binding.editProfilePlaceholder.setOnClickListener {
            findNavController().navigate(R.id.action_configuracaoFragment_to_profileFragment,  Bundle().apply {
                putParcelable(getString(R.string.image_uri_key), userPictureUri)
            })
        }

        loginViewModel.userLoggedOffState.observe(viewLifecycleOwner){ success ->
            if(success){
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                requireActivity().startActivity(intent)
                requireActivity().finish()
            }else{
                Snackbar.make(requireView(), Constants.EMPTY_STRING, Snackbar.LENGTH_SHORT).show()
            }
        }

        configViewModel.configData.observe(viewLifecycleOwner){ configData ->
            binding.txvHourLastBackup.text = configData.dateLastBackup.ifBlank { getString(R.string.sem_backup_realizado) }
            binding.toggleCartTotal.isChecked = configData.isToShowCartTotal
            binding.toggleProductsDone.isChecked = configData.isToMoveItensToBottom
            binding.toggleAutoBackup.isChecked = configData.isAutoBackupOn
            binding.profileName.text = configData.userName
            configData?.userProfilePicture?.let {
                userPictureUri = it
                Glide.with(requireContext())
                    .load(it)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.profilePicture)
            }
            binding.loadingProgress.isVisible(false)
            binding.loadingProfilePicture.isVisible(false)
        }
    }

    private fun navigateBack(){
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}