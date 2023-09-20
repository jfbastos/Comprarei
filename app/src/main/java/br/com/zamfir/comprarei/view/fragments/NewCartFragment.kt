package br.com.zamfir.comprarei.view.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentNewCartBinding
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.DateInputMask
import br.com.zamfir.comprarei.util.DateUtil
import br.com.zamfir.comprarei.util.errorAnimation
import br.com.zamfir.comprarei.util.setVisibility
import java.util.*

class NewCartFragment() : DialogFragment() {

    private var _binding: FragmentNewCartBinding? = null
    private val binding: FragmentNewCartBinding get() = _binding!!
    private var cart : Cart? = null
    private var categories : List<Category> = listOf()
    private var lastKnowPosition : Int = -1

    constructor(cart : Cart, categories : List<Category>) : this(){
        this.cart = cart
        this.categories = categories
    }

    constructor(lastKnowPosition : Int, categories : List<Category>) : this(){
        this.lastKnowPosition = lastKnowPosition
        this.categories = categories
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = FragmentNewCartBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        if(categories.isNotEmpty()){
            val categoriesDescriptions = mutableListOf("Sem categoria")
            categories.map { it.description }.forEach {
                categoriesDescriptions.add(it)
            }
            val arrayAdapter = ArrayAdapter(requireContext(), R.layout.category_item_menu, categoriesDescriptions)
            binding.categoriesMenu.setAdapter(arrayAdapter)
        }else{
            binding.categoryMenuItem.hint = getString(R.string.no_categories)
            binding.categoryMenuItem.isEnabled = false
            binding.categoryMenuItem.isClickable = false
        }

        cart?.let {
            binding.cartName.setText(it.name)
            binding.cartDate.setText(it.data)
            binding.cartStore.setText(it.store)
            it.category?.let {category ->
                binding.categoryMenuItem.apply {
                    startIconDrawable?.setTint(category.color)
                }
                binding.categoriesMenu.setText(category.description, false)
                binding.categoryMenuItem.requestFocus()
            } ?: run {
                binding.categoryMenuItem.startIconDrawable?.setTint(Color.WHITE)
            }
        }

        dateHandler()

        binding.categoriesMenu.setOnItemClickListener { _, _, _, _ ->
            categories.find { it.description == binding.categoriesMenu.text.toString() }?.let {
                binding.categoryMenuItem.startIconDrawable?.apply {
                    setTint(it.color)
                }
            } ?: run {
                binding.categoryMenuItem.startIconDrawable?.setTint(Color.WHITE)
            }
        }

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
                        cart?.apply {
                            name = binding.cartName.text.toString()
                            data = binding.cartDate.text.toString().takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate()
                            categoryId = categories.find { it.description == binding.categoriesMenu.text.toString() }?.id ?: 0
                            category = categories.find { it.description == binding.categoriesMenu.text.toString() }
                            store = binding.cartStore.text.toString()
                        }
                    }else{
                        Cart(
                            name = binding.cartName.text.toString(),
                            data = binding.cartDate.text.toString().takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate(),
                            total = Constants.EMPTY_CART_VALUE,
                            position = lastKnowPosition,
                            categoryId = categories.find { it.description == binding.categoriesMenu.text.toString() }?.id ?: 0,
                            store = binding.cartStore.text.toString()).apply {
                                category = categories.find { it.description == binding.categoriesMenu.text.toString() }
                        }
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




