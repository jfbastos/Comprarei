package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import br.com.iesb.comprarei.databinding.FragmentNewCartBinding
import br.com.iesb.comprarei.util.DateInputMask
import br.com.iesb.comprarei.util.DateUtil
import br.com.iesb.comprarei.util.errorAnimation
import br.com.iesb.comprarei.util.setVisibility
import br.com.iesb.comprarei.viewmodel.CartViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class NewCartFragment : androidx.fragment.app.DialogFragment() {

    private var _binding: FragmentNewCartBinding? = null
    private val binding: FragmentNewCartBinding get() = _binding!!

    private val viewModel: CartViewModel by viewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = FragmentNewCartBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        dateHandler()

        binding.btnOk.setOnClickListener {
            when {
                binding.cartName.text.isNullOrBlank() -> {
                    binding.cartName.errorAnimation()

                }
                binding.cartDate.text.isNullOrBlank() -> {
                    binding.cartDate.errorAnimation()
                }

                DateUtil.validateDate(binding.cartDate.text.toString()) -> {
                    viewModel.saveCart(
                        binding.cartName.text.toString(),
                        binding.cartDate.text.toString()
                    )
                    this.dismiss()
                }
                else -> {
                    binding.cartDate.errorAnimation()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            this.dismiss()
        }

        return builder.create()
    }

    private fun dateHandler() {

        DateInputMask(binding.cartDate).listen()

        binding.datePicker.init(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ) { _, year, month, day ->

            val formattedDay = if (day < 10) "0${day}" else "$day"
            val formattedMonth = if (month < 10) "0${month + 1}" else "${month + 1}"

            val date = "$formattedDay/$formattedMonth/$year"

            binding.cartDate.setText(date)
            binding.datePicker.setVisibility(false)
            binding.newCartForm.setVisibility(true)
        }

        binding.cartDateLayout.setEndIconOnClickListener {
            binding.newCartForm.setVisibility(false)
            binding.datePicker.setVisibility(true)
        }
    }

    override fun onDetach() {
        super.onDetach()
        _binding = null
    }
}




