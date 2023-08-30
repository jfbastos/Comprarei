package br.com.iesb.comprarei.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import androidx.annotation.StyleRes
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import br.com.iesb.comprarei.databinding.FragmentNewCartBinding
import br.com.iesb.comprarei.model.Cart
import br.com.iesb.comprarei.util.*
import br.com.iesb.comprarei.viewmodel.CartViewModel
import com.google.android.material.resources.TextAppearance
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class NewCartFragment() : DialogFragment() {

    private var _binding: FragmentNewCartBinding? = null
    private val binding: FragmentNewCartBinding get() = _binding!!
    private var cart : Cart? = null
    private var lastKnowPosition : Int = -1

    constructor(cart : Cart) : this(){
        this.cart = cart
    }

    constructor(lastKnowPosition : Int) : this(){
        this.lastKnowPosition = lastKnowPosition
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = FragmentNewCartBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        cart?.let {
            binding.cartName.setText(it.name)
            binding.cartDate.setText(it.data)
        }

        dateHandler()

        binding.cartName.doAfterTextChanged {
            if(binding.cartNameLayout.error?.isNotBlank() == true) binding.cartNameLayout.isErrorEnabled = false
        }

        binding.btnOk.setOnClickListener {
            when {
                binding.cartName.text.isNullOrBlank() -> {
                    binding.cartNameLayout.isErrorEnabled = true
                    binding.cartNameLayout.errorAnimation()
                    binding.cartNameLayout.error = "Dê um nome ao seu carrinho!"
                }

                DateUtil.validateDate(binding.cartDate.text.toString()) -> {
                    cart = if(cart != null){
                        cart!!.name = binding.cartName.text.toString()
                        cart!!.data = binding.cartDate.text.toString().takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate()
                        cart
                    }else{
                        Cart(
                            name = binding.cartName.text.toString(),
                            data = binding.cartDate.text.toString().takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate(),
                            total = Constants.EMPTY_CART_VALUE,
                            position = lastKnowPosition)
                    }

                    setFragmentResult(Constants.NEW_CART_KEY, bundleOf(Constants.CART_BUNDLE_KEY to cart))
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




