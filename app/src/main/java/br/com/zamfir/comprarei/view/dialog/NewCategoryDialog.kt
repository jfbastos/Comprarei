package br.com.zamfir.comprarei.view.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.DialogNewCategoryBinding
import br.com.zamfir.comprarei.data.model.entity.Category
import br.com.zamfir.comprarei.util.isVisible
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder

class NewCategoryDialog() : DialogFragment() {

    private var _binding : DialogNewCategoryBinding? = null
    private val binding : DialogNewCategoryBinding get() = _binding!!
    private var categoryColor : Int = 0
    private var category : Category? = null
    private lateinit var callback : (Category, Boolean) -> Unit
    private var existingCategories = listOf<Category>()

    constructor(categories : List<Category>, category: Category, callback : (Category, Boolean) -> Unit) : this(){
        this.existingCategories = categories
        this.category = category
        this.callback = callback
    }

    constructor(categories : List<Category>, callback: (Category, Boolean) -> Unit) : this(){
        this.existingCategories = categories
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
            binding.dialogTitle.text = getString(R.string.edit_category)
            binding.categoryName.setText(category.description)
            binding.imageCategoryColor.isVisible(true)
            binding.imageCategoryColor.setColorFilter(category.color, android.graphics.PorterDuff.Mode.MULTIPLY)
            categoryColor = category.color
            binding.deleteButton.isVisible(true)

            binding.deleteButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_confirmation)
                    .setMessage(getString(R.string.message_delete_confirmation) + category.description + " ?")
                    .setPositiveButton(R.string.positive_confirmation){ dialog, _ ->
                        callback.invoke(category, true)
                        dialog.dismiss()
                        dismissThis()
                    }
                    .setNegativeButton(R.string.negative_confirmation){dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }

        binding.btnOk.setOnClickListener {
            when {
                binding.categoryName.text.isNullOrBlank() -> {
                    showWarningInfo(true, getString(R.string.blank_name))
                }
                isNewCategoryAndAlreadyExists() -> showWarningInfo(true, getString(R.string.category_name_already_exists))
                categoryColor == 0 -> showWarningInfo(true, getString(R.string.select_category_color))
                else -> {
                    category?.apply{
                        description = binding.categoryName.text.toString()
                        color = categoryColor
                        callback.invoke(this, false)
                    } ?: run{
                        callback.invoke(Category(description = binding.categoryName.text.toString(), color = categoryColor), false)
                    }
                    dismissThis()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            dismissThis()
        }

        isCancelable = false

        return builder.create()
    }

    private fun isNewCategoryAndAlreadyExists() = existingCategories.map { it.description.uppercase() }.contains(binding.categoryName.text.toString().uppercase()) && category == null

    private fun dismissThis(){
        isCancelable = true
        dismiss()
    }

    private fun showWarningInfo(isOnlyWarning : Boolean = true, msg : String) {
        binding.warningPlaceholder.isVisible(true)
        binding.warningMsg.text = msg
        if(!isOnlyWarning){
            binding.warningIcon.setImageResource(R.drawable.round_warning_24_red)
            binding.warningPlaceholder.background = ResourcesCompat.getDrawable(requireContext().resources, R.drawable.error_background, null)
            binding.warningMsg.setTextColor(requireActivity().resources.getColor(R.color.delete_red_text, null))
        }else{
            binding.warningIcon.setImageResource(R.drawable.round_warning_24_yellow)
            binding.warningPlaceholder.background = ResourcesCompat.getDrawable(requireContext().resources, R.drawable.warning_background, null)
            binding.warningMsg.setTextColor(requireActivity().resources.getColor(R.color.warning_yellow_text, null))
        }
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
                binding.imageCategoryColor.isVisible(true)
                binding.imageCategoryColor.setColorFilter(selectedColor, android.graphics.PorterDuff.Mode.MULTIPLY)
                categoryColor = selectedColor
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel) { dialog, which ->
                dialog.dismiss()
            }.build().show()
    }
}

