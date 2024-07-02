package br.com.zamfir.comprarei.view.components

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.util.Pair
import androidx.core.view.isVisible
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.databinding.FilterDialogBinding
import br.com.zamfir.comprarei.model.entity.ByCategory
import br.com.zamfir.comprarei.model.entity.ByDate
import br.com.zamfir.comprarei.model.entity.ByValue
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.model.entity.Filter
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.convertMonetaryToDouble
import br.com.zamfir.comprarei.util.setMonetary
import br.com.zamfir.comprarei.util.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FilterDialog() : BottomSheetDialogFragment() {

    private var _binding : FilterDialogBinding? = null
    val binding : FilterDialogBinding get() = _binding!!
    var categories : List<Category> = listOf()
    private lateinit var filterCallback : (filter : Filter, isClearFilter : Boolean) -> Unit
    private lateinit var filter : Filter

    constructor(filter : Filter, categories : List<Category>, filterCallback : (filter : Filter, isClearFilter : Boolean) -> Unit) : this(){
        this.filter = filter
        this.categories = categories
        this.filterCallback = filterCallback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FilterDialogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryFilter()

        sort()

        valueFilter()

        dateFilter()

        filter.also{ filter ->
            when (filter.sortOption) {
                Constants.FILTER_NAME -> {
                    binding.chipName.isChecked = true
                }

                Constants.FILTER_DATE -> {
                    binding.chipDate.isChecked = true
                }

                Constants.FILTER_CATEGORY -> {
                    binding.chipCategory.isChecked = true
                }

                Constants.FILTER_VALUE_HIGH -> {
                    binding.chipValueHigh.isChecked = true
                }

                Constants.FILTER_VALUE_LOW -> {
                    binding.chipValueLow.isChecked = true
                }
            }

            binding.chipCategory.isVisible(categories.isNotEmpty())

            binding.categoryLayoutPlaceholder.isVisible(categories.isNotEmpty())

            filter.byDate?.let { date ->
                binding.datePickerBtn.text = "${formatDate(date.startDate)} ${getString(R.string.to)} ${formatDate(date.endDate)}"
            }

            filter.byValue?.let {value ->
                binding.minValue.setMonetary(value.valueMin.toString())
                binding.maxValue.setMonetary(value.valueMax.toString())
                binding.valueOperator.setText(value.operator, false)
                binding.valueMaxLayout.isVisible(binding.valueOperator.text.toString() == getString(
                    R.string.range
                ))
            }

            filter.byCategory?.let {
                binding.categoriesMenu.setText(it.category.description, false)
            }
        }

        binding.applyFilterBtn.setOnClickListener {
            if(!binding.minValue.text.isNullOrBlank()){
                filter.byValue = when(binding.valueOperator.text.toString()){
                    "Equal" -> ByValue(Constants.OPERATOR_EQUAL, binding.minValue.text.toString().convertMonetaryToDouble(), if(binding.maxValue.isVisible) binding.maxValue.text.toString().convertMonetaryToDouble() else 0.0)
                    "Less than" -> ByValue(Constants.OPERATOR_LESS_THAN, binding.minValue.text.toString().convertMonetaryToDouble(), if(binding.maxValue.isVisible) binding.maxValue.text.toString().convertMonetaryToDouble() else 0.0)
                    "Greater then" -> ByValue(Constants.OPERATOR_GRATER_THAN, binding.minValue.text.toString().convertMonetaryToDouble(), if(binding.maxValue.isVisible) binding.maxValue.text.toString().convertMonetaryToDouble() else 0.0)
                    "Range" -> ByValue(Constants.OPERATOR_RANGE, binding.minValue.text.toString().convertMonetaryToDouble(), if(binding.maxValue.isVisible) binding.maxValue.text.toString().convertMonetaryToDouble() else 0.0)
                    else -> null
                }
            }

            filterCallback.invoke(this.filter, false)
            dismiss()
        }

        binding.clearFilterBtn.setOnClickListener {
            binding.chipGroup.clearCheck()
            binding.datePickerBtn.text = getText(R.string.select_date_range)
            binding.valueOperator.setText(Constants.OPERATOR_EQUAL, false)
            binding.minValue.setMonetary("0")
            binding.maxValue.setMonetary("0")
            binding.categoriesMenu.setText("", false)
            filter.byDate = null
            filter.byValue = null
            filter.byCategory = null
            filter.sortOption = null
        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun formatDate(localDate : LocalDate) : String{
        val format = "dd/MM/yyyy"
        return localDate.format(DateTimeFormatter.ofPattern(format))
    }

    private fun valueFilter() {
        binding.minValue.setMonetary(initialValue = "0")
        binding.maxValue.setMonetary(initialValue = "0")
        binding.valueOperator.setOnItemClickListener { _, _, position, _ ->
            binding.valueMaxLayout.isVisible(position == 3)
            if (position != 3) binding.valueMinLayout.hint = getString(R.string.value) else binding.valueMinLayout.hint =
                getString(
                    R.string.min
                )
        }

        if(binding.valueOperator.text.toString().isBlank()) binding.valueOperator.setText(Constants.OPERATOR_EQUAL, false)

    }

    private fun dateFilter() {
        binding.datePickerBtn.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker().setTitleText(getString(R.string.select_dates)).setSelection(
                Pair(
                    MaterialDatePicker.thisMonthInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()
                )
            ).build()

            datePicker.show(parentFragmentManager, "")

            datePicker.addOnPositiveButtonClickListener {
                val startDate = parseDate(it.first)
                val endDate = parseDate(it.second)
                binding.datePickerBtn.text = "$startDate ${getString(R.string.to)} $endDate"
                filter.byDate = ByDate(LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")), LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            }
        }
    }

    private fun sort() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            when (group.checkedChipId) {
                binding.chipName.id -> {
                    filter.sortOption = Constants.FILTER_NAME
                }

                binding.chipDate.id -> {
                    filter.sortOption = Constants.FILTER_DATE
                }

                binding.chipCategory.id -> {
                    filter.sortOption = Constants.FILTER_CATEGORY
                }

                binding.chipValueHigh.id -> {
                    filter.sortOption = Constants.FILTER_VALUE_HIGH
                }

                binding.chipValueLow.id -> {
                    filter.sortOption = Constants.FILTER_VALUE_LOW
                }
            }
        }
    }

    private fun categoryFilter() {
        if (categories.isNotEmpty()) {
            val arrayAdapter = ArrayAdapter(requireContext(), R.layout.category_item_menu, categories.map { it.description })
            binding.categoriesMenu.setAdapter(arrayAdapter)
        } else {
            binding.categoryMenuLayout.hint = getString(R.string.no_categories)
            binding.categoryMenuLayout.isEnabled = false
            binding.categoryMenuLayout.isClickable = false
        }

        binding.categoriesMenu.setOnItemClickListener { _, _, _, _ ->
            categories.find { it.description == binding.categoriesMenu.text.toString() }?.let {
                binding.categoryMenuLayout.startIconDrawable?.apply {
                    setTint(it.color)
                }
                filter.byCategory = ByCategory(it)
            } ?: run {
                binding.categoryMenuLayout.startIconDrawable?.setTint(Color.WHITE)
            }
        }
    }

    private fun parseDate(it : Long) : String{
        val utcTime = Date(it)
        val format = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(utcTime)
    }
}