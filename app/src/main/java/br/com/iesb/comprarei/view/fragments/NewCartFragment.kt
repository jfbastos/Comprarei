package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import br.com.iesb.comprarei.databinding.FragmentNewCartBinding
import br.com.iesb.comprarei.model.Repository
import br.com.iesb.comprarei.util.FormatFrom
import br.com.iesb.comprarei.util.Validator
import br.com.iesb.comprarei.util.errorAnimation
import br.com.iesb.comprarei.viewmodel.MainViewModel
import br.com.iesb.comprarei.viewmodel.ViewModelFactory
import java.time.Instant
import java.util.*


class NewCartFragment : DialogFragment() {

    private var _binding: FragmentNewCartBinding? = null
    private val binding: FragmentNewCartBinding get() = _binding!!

    private val viewModel: MainViewModel by lazy {
        val viewModelProviderFactory = ViewModelFactory(Repository())
        ViewModelProvider(this, viewModelProviderFactory)[MainViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = FragmentNewCartBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        preSetDate()

        binding.btnOk.setOnClickListener {
            when {
                binding.cartName.text.isNullOrBlank() -> {
                    binding.cartName.apply {
                        errorAnimation()
                        error = "Can't be blank!"
                    }
                }
                binding.cartDate.text.isNullOrBlank() -> {
                    binding.cartDate.apply {
                        errorAnimation()
                        error = "Can't be blank!"
                    }
                }
                Validator.validateDate(binding.cartDate.text.toString()) -> {
                    viewModel.saveCart(
                        binding.cartName.text.toString(),
                        binding.cartDate.text.toString()
                    )
                    this.dismiss()
                }
                else -> {
                    binding.cartDate.apply {
                        errorAnimation()
                        error = "Invalid date!"
                    }
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            this.dismiss()
        }

        return builder.create()
    }

    private fun preSetDate() {
        val todayDate = FormatFrom.stringToData(Date.from(Instant.now()))
        if (todayDate != "No date") {
            binding.cartDate.setText(todayDate)
        }
    }

    override fun onDetach() {
        super.onDetach()
        _binding = null
    }


}


