package br.com.zamfir.comprarei.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FragmentNewCartBinding
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.DateInputMask
import br.com.zamfir.comprarei.util.DateUtil
import br.com.zamfir.comprarei.util.errorAnimation
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar

class NewCartFragment() : DialogFragment() {

    private var _binding: FragmentNewCartBinding? = null
    private val binding: FragmentNewCartBinding get() = _binding!!
    private var cart : Cart? = null
    private var categories : List<Category> = listOf()
    private var lastKnowPosition : Int = -1
    private lateinit var onSave : (Cart) -> Unit
    private lateinit var onUpdate : (Cart) -> Unit
    private var isUpdate = false

    constructor(cart : Cart, categories : List<Category>, onFinish : (Cart) -> Unit) : this(){
        this.cart = cart
        this.categories = categories
        this.onUpdate = onFinish
        this.isUpdate = true
    }

    constructor(lastKnowPosition : Int, categories : List<Category>, onFinish : (Cart) -> Unit) : this(){
        this.lastKnowPosition = lastKnowPosition
        this.categories = categories
        this.onSave = onFinish
        this.isUpdate = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = FragmentNewCartBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        if(categories.isNotEmpty()){
            val categoriesDescriptions = mutableListOf(getString(R.string.none_category))
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
                    binding.cartNameLayout.errorAnimation(getString(R.string.name_your_cart))
                }

                DateUtil.validateDate(binding.cartDate.text.toString()) -> {
                    val selectedCategory = categories.firstOrNull { it.description == binding.categoriesMenu.text.toString() }
                    if(isUpdate){
                        cart?.apply {
                            name = binding.cartName.text.toString()
                            data = binding.cartDate.text.toString().takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate()
                            categoryId = selectedCategory?.id ?: 0
                            category = selectedCategory
                            store = binding.cartStore.text.toString()
                            onUpdate.invoke(this)
                        }
                    }else{
                        onSave.invoke(Cart(
                            name = binding.cartName.text.toString(),
                            data = binding.cartDate.text.toString().takeIf { it.isNotBlank() } ?: DateUtil.getTodayDate(),
                            total = Constants.EMPTY_CART_VALUE,
                            position = lastKnowPosition,
                            categoryId = selectedCategory?.id ?: 0,
                            store = binding.cartStore.text.toString()).apply {
                            category = selectedCategory
                        })
                    }

                    this.dismiss()
                }
                else -> {
                    binding.cartDate.errorAnimation("")
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            this.dismiss()
        }

        return builder.create()
    }

    private fun dateHandler() {

        val materialDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setPositiveButtonText(R.string.ok)
            .build()

        materialDatePicker.addOnPositiveButtonClickListener { timeInMillis ->
            val retrievedData = Calendar.getInstance().apply {
                this.timeInMillis = timeInMillis
            }

            val day = retrievedData.get(Calendar.DAY_OF_MONTH)
            val month = retrievedData.get(Calendar.MONTH)
            val year = retrievedData.get(Calendar.YEAR)

            val formattedDay = if (day < 10) "0${day + 1}" else "${day + 1}"
            val formattedMonth = if (month < 10) "0${month + 1}" else "${month + 1}"

            val date = "$formattedDay/$formattedMonth/$year"

            binding.cartDate.setText(date)
            materialDatePicker.dismiss()
        }

        DateInputMask(binding.cartDate).listen()

        binding.cartDateLayout.setEndIconOnClickListener {
            materialDatePicker.show(parentFragmentManager, "")
        }
    }

    override fun onDetach() {
        super.onDetach()
        _binding = null
    }
}




