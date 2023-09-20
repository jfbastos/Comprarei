package br.com.zamfir.comprarei.view.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.DialogNewCategoryBinding
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.util.errorAnimation
import br.com.zamfir.comprarei.util.setVisibility
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.snackbar.Snackbar

class NewCategoryDialog() : DialogFragment() {


    private var _binding : DialogNewCategoryBinding? = null
    private val binding : DialogNewCategoryBinding get() = _binding!!
    private var categoryColor : Int = 0
    private var category : Category? = null
    private lateinit var callback : (Category, Boolean) -> Unit

    constructor(category: Category, callback : (Category, Boolean) -> Unit) : this(){
        this.category = category
        this.callback = callback
    }

    constructor(callback: (Category, Boolean) -> Unit) : this(){
        this.callback = callback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        _binding = DialogNewCategoryBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.colorSelect.setOnClickListener {
            callColorSelectionLib()
        }

        category?.let{ category ->
            binding.categoryName.setText(category.description)
            binding.imageCategoryColor.setColorFilter(category.color, android.graphics.PorterDuff.Mode.MULTIPLY)
            categoryColor = category.color
            binding.deleteButton.setVisibility(true)

            binding.deleteButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_confirmation)
                    .setMessage(getString(R.string.message_delete_confirmation) + category.description + " ?")
                    .setPositiveButton(R.string.positive_confirmation){ dialog, _ ->
                        callback.invoke(category, true)
                        dialog.dismiss()
                        dismiss()
                    }
                    .setNegativeButton(R.string.negative_confirmation){dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }



        binding.btnOk.setOnClickListener {
            when {
                binding.categoryName.text.isNullOrBlank() -> {
                    binding.categoryNameLayout.isErrorEnabled = true
                    binding.categoryNameLayout.errorAnimation()
                    binding.categoryNameLayout.error = getString(R.string.blank_name)
                    Toast.makeText(requireContext(), getString(R.string.blank_name), Toast.LENGTH_SHORT).show()
                }
                categoryColor == 0 -> Toast.makeText(requireContext(), getString(R.string.select_category_color), Toast.LENGTH_SHORT).show()
                else -> {
                    category?.apply{
                        description = binding.categoryName.text.toString()
                        color = categoryColor
                        callback.invoke(this, false)
                    } ?: run{
                        callback.invoke(Category(description = binding.categoryName.text.toString(), color = categoryColor), false)
                    }
                    dismiss()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }


        return builder.create()
    }

    private fun callColorSelectionLib(){
        ColorPickerDialogBuilder
            .with(context)
            .setTitle(getString(R.string.choose_color))
            .initialColor(Color.WHITE)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(10)
            .lightnessSliderOnly()
            .setOnColorSelectedListener {}
            .setPositiveButton(getString(R.string.ok)) { dialog, selectedColor, allColors ->
                binding.imageCategoryColor.setColorFilter(selectedColor, android.graphics.PorterDuff.Mode.MULTIPLY)
                categoryColor = selectedColor
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel) { dialog, which ->
                dialog.dismiss()
            }.build().show()
    }
}

