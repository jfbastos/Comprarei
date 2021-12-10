package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import br.com.iesb.comprarei.databinding.FragmentNewCartBinding
import br.com.iesb.comprarei.model.CartRepository
import br.com.iesb.comprarei.util.FormatFrom
import br.com.iesb.comprarei.util.Validator
import br.com.iesb.comprarei.util.errorAnimation
import br.com.iesb.comprarei.viewmodel.CartViewModel
import br.com.iesb.comprarei.viewmodel.factories.CartViewModelFactory
import java.time.Instant
import java.util.*


class NewCartFragment : DialogFragment() {

    private var _binding: FragmentNewCartBinding? = null
    private val binding: FragmentNewCartBinding get() = _binding!!

    private val viewModel: CartViewModel by lazy {
        val viewModelProviderFactory = CartViewModelFactory(CartRepository())
        ViewModelProvider(this, viewModelProviderFactory)[CartViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = FragmentNewCartBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        preSetDate()

        binding.btnOk.setOnClickListener {
            when {
                binding.cartName.text.isNullOrBlank() -> {
                    binding.cartName.errorAnimation("Can't be blank!")

                }
                binding.cartDate.text.isNullOrBlank() -> {
                    binding.cartDate.errorAnimation("Can't be blank!")

                }
                Validator.validateDate(binding.cartDate.text.toString()) -> {
                    viewModel.saveCart(
                        binding.cartName.text.toString(),
                        binding.cartDate.text.toString()
                    )
                    this.dismiss()
                }
                else -> {
                    binding.cartDate.errorAnimation("Invalid date!")
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


